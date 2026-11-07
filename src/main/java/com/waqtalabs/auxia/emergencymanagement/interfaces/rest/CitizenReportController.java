package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterCitizenReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterCitizenReportCommandHandler.Registration;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetCitizenReportProgressQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.CitizenReportResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.CitizenReportStatusResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterCitizenReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SOS of citizens from the mobile app, without signing in. The app keeps every SOS in a queue
 * until the API confirms it, and asks for its progress with the identifier it generated.
 */
@RestController
@RequestMapping("/api/v1/public/field-reports")
@SecurityRequirements
@Tag(name = "Citizen SOS", description = "SOS of citizens from the mobile app, without signing in")
public class CitizenReportController {

  private final RegisterCitizenReportCommandHandler registerCitizenReport;
  private final CitizenReportQueryService citizenReportQueries;

  public CitizenReportController(
      RegisterCitizenReportCommandHandler registerCitizenReport,
      CitizenReportQueryService citizenReportQueries) {
    this.registerCitizenReport = registerCitizenReport;
    this.citizenReportQueries = citizenReportQueries;
  }

  @PostMapping
  @Operation(
      summary = "Send an SOS",
      description =
          "Registers the SOS as a field report of source CITIZEN, pending review, in the active"
              + " emergency and the zone nearest to its location. Sending the same clientReportId"
              + " again returns the stored SOS. Each address can send 10 new SOS every 10"
              + " minutes; retries do not count.")
  @ApiResponse(responseCode = "201", description = "SOS stored")
  @ApiResponse(responseCode = "200", description = "The SOS was already stored")
  @ApiResponse(responseCode = "400", description = "Invalid SOS")
  @ApiResponse(responseCode = "429", description = "Too many SOS; see the Retry-After header")
  public ResponseEntity<CitizenReportStatusResource> send(
      @Valid @RequestBody RegisterCitizenReportRequest request, HttpServletRequest http) {
    RegisterCitizenReportCommand command =
        CitizenReportResourceAssembler.toCommand(request, ClientAddress.of(http));
    Registration registration;
    try {
      registration = registerCitizenReport.handle(command);
    } catch (DataIntegrityViolationException sameSosStoredMeanwhile) {
      // Two copies of the SOS arrived at the same time and the other one was stored first.
      registration = registerCitizenReport.handle(command);
    }
    CitizenReportStatusResource status = statusOf(registration.report().id());
    if (!registration.created()) {
      return ResponseEntity.ok(status);
    }
    return ResponseEntity.created(
            URI.create("/api/v1/public/field-reports/" + registration.report().id().value()))
        .body(status);
  }

  @GetMapping("/{clientReportId}")
  @Operation(
      summary = "Progress of an SOS",
      description =
          "RECEIVED while it has no zone or its zone has no score yet, PRIORITIZED once its zone"
              + " has a score or a manual priority, and BRIGADE_ON_THE_WAY once a brigade took an"
              + " approved plan of its zone. The score itself is not shown.")
  @ApiResponse(responseCode = "200", description = "SOS found")
  @ApiResponse(responseCode = "404", description = "No SOS has that identifier")
  public CitizenReportStatusResource status(@PathVariable UUID clientReportId) {
    return statusOf(new CitizenReportId(clientReportId));
  }

  private CitizenReportStatusResource statusOf(CitizenReportId id) {
    return CitizenReportResourceAssembler.toResource(
        citizenReportQueries.handle(new GetCitizenReportProgressQuery(id)));
  }
}
