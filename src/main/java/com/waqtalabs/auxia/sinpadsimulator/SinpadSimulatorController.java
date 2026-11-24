package com.waqtalabs.auxia.sinpadsimulator;

import com.waqtalabs.auxia.sinpadsimulator.SinpadRegistry.Registration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API of the SINPAD simulator. Every request carries the key of the sender in {@code X-Api-Key},
 * the same {@code SINPAD_API_KEY} that AuxIA sends. It runs unless {@code
 * auxia.sinpad.simulator.enabled} is false.
 */
@RestController
@ConditionalOnProperty(
    name = "auxia.sinpad.simulator.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequestMapping(SinpadSimulatorController.BASE_PATH + "/api/v1/reportes")
@SecurityRequirements
@Tag(
    name = "SINPAD simulator",
    description =
        "Stands in for SINPAD of INDECI, which has no public API. AuxIA sends the field reports"
            + " of an emergency here with POST /api/v1/emergencies/{id}/sinpad-submissions.")
class SinpadSimulatorController {

  static final String BASE_PATH = "/sinpad-simulator";
  static final String API_KEY_HEADER = "X-Api-Key";

  private final SinpadRegistry registry;
  private final byte[] apiKey;

  SinpadSimulatorController(
      SinpadRegistry registry, @Value("${auxia.sinpad.api-key}") String apiKey) {
    this.registry = registry;
    this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
  }

  @PostMapping
  @Operation(
      summary = "Receive the field reports of an emergency",
      description =
          "Returns the registration number. Sending the same codigoExterno again replaces its"
              + " reports and keeps the number.")
  @ApiResponse(responseCode = "201", description = "Emergency registered")
  @ApiResponse(responseCode = "200", description = "Reports of a registered emergency replaced")
  @ApiResponse(responseCode = "400", description = "A required field is missing")
  @ApiResponse(responseCode = "401", description = "Missing or wrong X-Api-Key")
  ResponseEntity<SinpadReceipt> receive(
      @RequestHeader(name = API_KEY_HEADER, required = false) String key,
      @Valid @RequestBody SinpadSubmission submission) {
    checkKey(key);
    Registration registration = registry.receive(submission);
    return ResponseEntity.status(registration.isNew() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(registration.receipt());
  }

  @GetMapping("/{codigoRegistro}")
  @Operation(summary = "Get a registration with the reports it holds")
  @ApiResponse(responseCode = "200", description = "The registration")
  @ApiResponse(responseCode = "401", description = "Missing or wrong X-Api-Key")
  @ApiResponse(responseCode = "404", description = "No registration with that number")
  SinpadSubmission find(
      @RequestHeader(name = API_KEY_HEADER, required = false) String key,
      @PathVariable String codigoRegistro) {
    checkKey(key);
    return registry
        .find(codigoRegistro)
        .map(Registration::submission)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Compared in constant time, so the answer time does not reveal the key. */
  private void checkKey(String key) {
    if (key == null || !MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }
}
