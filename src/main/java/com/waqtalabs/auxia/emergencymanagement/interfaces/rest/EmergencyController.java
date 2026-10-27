package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.CloseEmergencyCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterEmergencyCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.CloseEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetActiveEmergenciesQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetEmergencyByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.EmergencyQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.EmergencyResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.EmergencyResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterEmergencyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emergencies")
@Tag(name = "Emergencies", description = "Disasters and their administrative location")
public class EmergencyController {

  private final RegisterEmergencyCommandHandler registerEmergency;
  private final CloseEmergencyCommandHandler closeEmergency;
  private final EmergencyQueryService emergencyQueries;

  public EmergencyController(
      RegisterEmergencyCommandHandler registerEmergency,
      CloseEmergencyCommandHandler closeEmergency,
      EmergencyQueryService emergencyQueries) {
    this.registerEmergency = registerEmergency;
    this.closeEmergency = closeEmergency;
    this.emergencyQueries = emergencyQueries;
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping
  @Operation(summary = "Register an emergency", description = "It starts in ACTIVE status.")
  @ApiResponse(responseCode = "201", description = "Emergency registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  public ResponseEntity<EmergencyResource> register(
      @Valid @RequestBody RegisterEmergencyRequest request) {
    Emergency emergency = registerEmergency.handle(EmergencyResourceAssembler.toCommand(request));
    return ResponseEntity.created(URI.create("/api/v1/emergencies/" + emergency.id().value()))
        .body(EmergencyResourceAssembler.toResource(emergency));
  }

  @GetMapping
  @Operation(summary = "List the active emergencies", description = "From the most recent.")
  public List<EmergencyResource> findActive() {
    return emergencyQueries.handle(new GetActiveEmergenciesQuery()).stream()
        .map(EmergencyResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/{emergencyId}")
  @Operation(summary = "Get an emergency")
  @ApiResponse(responseCode = "200", description = "Emergency found")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  public EmergencyResource findById(@PathVariable UUID emergencyId) {
    return EmergencyResourceAssembler.toResource(
        emergencyQueries.handle(new GetEmergencyByIdQuery(new EmergencyId(emergencyId))));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PatchMapping("/{emergencyId}/close")
  @Operation(
      summary = "Close an emergency",
      description = "A closed emergency does not accept new zones and cannot be reopened.")
  @ApiResponse(responseCode = "200", description = "Emergency closed")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  @ApiResponse(responseCode = "409", description = "The emergency is already closed")
  public EmergencyResource close(@PathVariable UUID emergencyId) {
    return EmergencyResourceAssembler.toResource(
        closeEmergency.handle(new CloseEmergencyCommand(new EmergencyId(emergencyId))));
  }
}
