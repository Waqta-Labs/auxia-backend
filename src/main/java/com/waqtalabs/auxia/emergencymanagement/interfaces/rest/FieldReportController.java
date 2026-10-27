package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AssignFieldReportZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.StructureFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignFieldReportZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.StructureFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportsByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportsByZoneIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.FieldReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.FieldReportResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.AssignFieldReportZoneRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.FieldReportResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterEmergencyFieldReportRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterFieldReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Field reports",
    description = "Reports written in natural language and their NLP structuring (US-01, US-02)")
public class FieldReportController {

  private final RegisterFieldReportCommandHandler registerFieldReport;
  private final AssignFieldReportZoneCommandHandler assignFieldReportZone;
  private final StructureFieldReportCommandHandler structureFieldReport;
  private final FieldReportQueryService fieldReportQueries;

  public FieldReportController(
      RegisterFieldReportCommandHandler registerFieldReport,
      AssignFieldReportZoneCommandHandler assignFieldReportZone,
      StructureFieldReportCommandHandler structureFieldReport,
      FieldReportQueryService fieldReportQueries) {
    this.registerFieldReport = registerFieldReport;
    this.assignFieldReportZone = assignFieldReportZone;
    this.structureFieldReport = structureFieldReport;
    this.fieldReportQueries = fieldReportQueries;
  }

  @PostMapping("/zones/{zoneId}/field-reports")
  @Operation(
      summary = "Register a field report of a zone",
      description =
          "The report is structured with NLP right away, and the response already includes the"
              + " structured data unless the AI Service did not answer.")
  @ApiResponse(responseCode = "201", description = "Report registered and associated to the zone")
  @ApiResponse(responseCode = "400", description = "Invalid request, for example without text")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ResponseEntity<FieldReportResource> registerForZone(
      @PathVariable UUID zoneId, @Valid @RequestBody RegisterFieldReportRequest request) {
    return created(FieldReportResourceAssembler.toCommand(zoneId, request));
  }

  @PostMapping("/emergencies/{emergencyId}/field-reports")
  @Operation(
      summary = "Register a field report of an emergency",
      description =
          "With zoneId, the report is associated to that zone. Without it, the zone is"
              + " identified from the text; if it cannot be identified, the report stays pending"
              + " zone assignment.")
  @ApiResponse(responseCode = "201", description = "Report registered")
  @ApiResponse(responseCode = "400", description = "Invalid request, for example without text")
  @ApiResponse(responseCode = "404", description = "The emergency or the zone does not exist")
  @ApiResponse(responseCode = "422", description = "The zone belongs to another emergency")
  public ResponseEntity<FieldReportResource> registerForEmergency(
      @PathVariable UUID emergencyId,
      @Valid @RequestBody RegisterEmergencyFieldReportRequest request) {
    return created(FieldReportResourceAssembler.toCommand(emergencyId, request));
  }

  @GetMapping("/zones/{zoneId}/field-reports")
  @Operation(summary = "List the field reports of a zone", description = "From the most recent.")
  @ApiResponse(responseCode = "200", description = "Reports of the zone")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public List<FieldReportResource> findByZone(@PathVariable UUID zoneId) {
    return toResources(
        fieldReportQueries.handle(new GetFieldReportsByZoneIdQuery(new ZoneId(zoneId))));
  }

  @GetMapping("/emergencies/{emergencyId}/field-reports")
  @Operation(
      summary = "List the field reports of an emergency",
      description = "From the most recent. Filter the ones still pending zone assignment.")
  @ApiResponse(responseCode = "200", description = "Reports of the emergency")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  public List<FieldReportResource> findByEmergency(
      @PathVariable UUID emergencyId,
      @Parameter(description = "true to list only the reports without a zone")
          @RequestParam(defaultValue = "false")
          boolean pendingZoneAssignment) {
    var query =
        new GetFieldReportsByEmergencyIdQuery(new EmergencyId(emergencyId), pendingZoneAssignment);
    return toResources(fieldReportQueries.handle(query));
  }

  @GetMapping("/field-reports/{fieldReportId}")
  @Operation(summary = "Get a field report with its structured data")
  @ApiResponse(responseCode = "200", description = "Report found")
  @ApiResponse(responseCode = "404", description = "The report does not exist")
  public FieldReportResource findById(@PathVariable UUID fieldReportId) {
    return FieldReportResourceAssembler.toResource(
        fieldReportQueries.handle(new GetFieldReportByIdQuery(new FieldReportId(fieldReportId))));
  }

  @PostMapping("/field-reports/{fieldReportId}/structure")
  @Operation(
      summary = "Structure a field report with NLP",
      description =
          "Reports are structured on their own when they are registered (US-02). Use this"
              + " endpoint when the AI Service did not answer at that moment. The score of the"
              + " zone of the report is recalculated afterwards.")
  @ApiResponse(responseCode = "200", description = "Report structured")
  @ApiResponse(responseCode = "404", description = "The report does not exist")
  @ApiResponse(responseCode = "409", description = "The report is already structured")
  @ApiResponse(responseCode = "503", description = "The AI Service is not available")
  public FieldReportResource structure(@PathVariable UUID fieldReportId) {
    var command = new StructureFieldReportCommand(new FieldReportId(fieldReportId));
    return FieldReportResourceAssembler.toResource(structureFieldReport.handle(command));
  }

  @PatchMapping("/field-reports/{fieldReportId}/zone")
  @Operation(
      summary = "Assign the zone of a pending field report",
      description = "The zone must belong to the emergency of the report.")
  @ApiResponse(responseCode = "200", description = "Zone assigned")
  @ApiResponse(responseCode = "404", description = "The report or the zone does not exist")
  @ApiResponse(responseCode = "409", description = "The report already has a zone")
  @ApiResponse(responseCode = "422", description = "The zone belongs to another emergency")
  public FieldReportResource assignZone(
      @PathVariable UUID fieldReportId, @Valid @RequestBody AssignFieldReportZoneRequest request) {
    var command =
        new AssignFieldReportZoneCommand(
            new FieldReportId(fieldReportId), new ZoneId(request.zoneId()));
    return FieldReportResourceAssembler.toResource(assignFieldReportZone.handle(command));
  }

  private ResponseEntity<FieldReportResource> created(RegisterFieldReportCommand command) {
    FieldReport report = registerFieldReport.handle(command);
    FieldReport current = fieldReportQueries.handle(new GetFieldReportByIdQuery(report.id()));
    return ResponseEntity.created(URI.create("/api/v1/field-reports/" + report.id().value()))
        .body(FieldReportResourceAssembler.toResource(current));
  }

  private static List<FieldReportResource> toResources(List<FieldReport> reports) {
    return reports.stream().map(FieldReportResourceAssembler::toResource).toList();
  }
}
