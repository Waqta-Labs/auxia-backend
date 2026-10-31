package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.CalculateUrgencyScoreCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.UpdateZonePopulationImpactCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.ZoneManualPriorityCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.CalculateUrgencyScoreCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.PrioritizeZoneManuallyCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RemoveZoneManualPriorityCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneHistoryQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneRankingQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneReportQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZonesInChargeQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneHistoryQueryService;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneQueryService;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.ZoneResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ManualPriorityRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.PopulationImpactResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterZoneRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneDecisionResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneRankingItemResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneScoreResource;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Zones", description = "Areas affected by an emergency, their impact and urgency")
public class ZoneController {

  private final RegisterZoneCommandHandler registerZone;
  private final UpdateZonePopulationImpactCommandHandler updatePopulationImpact;
  private final CalculateUrgencyScoreCommandHandler calculateUrgencyScore;
  private final ZoneQueryService zoneQueries;
  private final ZoneReportQueryService zoneReports;
  private final ZoneManualPriorityCommandHandler manualPriority;
  private final ZoneHistoryQueryService zoneHistory;

  public ZoneController(
      RegisterZoneCommandHandler registerZone,
      UpdateZonePopulationImpactCommandHandler updatePopulationImpact,
      CalculateUrgencyScoreCommandHandler calculateUrgencyScore,
      ZoneQueryService zoneQueries,
      ZoneReportQueryService zoneReports,
      ZoneManualPriorityCommandHandler manualPriority,
      ZoneHistoryQueryService zoneHistory) {
    this.registerZone = registerZone;
    this.updatePopulationImpact = updatePopulationImpact;
    this.calculateUrgencyScore = calculateUrgencyScore;
    this.zoneQueries = zoneQueries;
    this.zoneReports = zoneReports;
    this.manualPriority = manualPriority;
    this.zoneHistory = zoneHistory;
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PutMapping("/zones/{zoneId}/manual-priority")
  @Operation(
      summary = "Mark a zone as a priority by hand",
      description =
          "For a risk the score does not capture, such as an imminent one. The zone goes first"
              + " in the ranking and shows who marked it, when and why (US-07).")
  @ApiResponse(responseCode = "200", description = "Zone prioritized")
  @ApiResponse(responseCode = "400", description = "The justification is missing")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ZoneResource prioritizeManually(
      @PathVariable UUID zoneId,
      @Valid @RequestBody ManualPriorityRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new PrioritizeZoneManuallyCommand(
            new ZoneId(zoneId), new AuthorityId(user.userId()), request.reason());
    return ZoneResourceAssembler.toResource(manualPriority.handle(command));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @DeleteMapping("/zones/{zoneId}/manual-priority")
  @Operation(
      summary = "Take the manual priority off a zone",
      description = "The score orders the zone again.")
  @ApiResponse(responseCode = "200", description = "Manual priority removed")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ZoneResource removeManualPriority(
      @PathVariable UUID zoneId, @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new RemoveZoneManualPriorityCommand(new ZoneId(zoneId), new AuthorityId(user.userId()));
    return ZoneResourceAssembler.toResource(manualPriority.handle(command));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping("/emergencies/{emergencyId}/zones")
  @Operation(
      summary = "Register a zone of an emergency",
      description =
          "It starts REGISTERED, with the signed-in authority as its responsible. With its"
              + " population, the urgency score is calculated right away and the response already"
              + " includes it, unless the AI Service did not answer.")
  @ApiResponse(responseCode = "201", description = "Zone registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  @ApiResponse(responseCode = "409", description = "The emergency is closed")
  @ApiResponse(responseCode = "422", description = "Inconsistent population impact")
  public ResponseEntity<ZoneResource> register(
      @PathVariable UUID emergencyId,
      @Valid @RequestBody RegisterZoneRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        ZoneResourceAssembler.toCommand(emergencyId, request, new AuthorityId(user.userId()));
    Zone zone = registerZone.handle(command);
    Zone current = zoneQueries.handle(new GetZoneByIdQuery(zone.id()));
    return ResponseEntity.created(URI.create("/api/v1/zones/" + zone.id().value()))
        .body(ZoneResourceAssembler.toResource(current));
  }

  @GetMapping("/emergencies/{emergencyId}/zones")
  @Operation(
      summary = "Rank the zones of an emergency by urgency",
      description =
          "From the highest to the lowest urgency score (US-06). Zones without a score go last,"
              + " in registration order. A score range leaves them out.")
  @ApiResponse(responseCode = "200", description = "Zones of the emergency")
  @ApiResponse(responseCode = "400", description = "A score limit is outside 0 to 100")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  public List<ZoneRankingItemResource> rank(
      @PathVariable UUID emergencyId,
      @Parameter(description = "Lowest score to include")
          @RequestParam(required = false)
          @DecimalMin("0")
          @DecimalMax("100")
          Double minScore,
      @Parameter(description = "Highest score to include")
          @RequestParam(required = false)
          @DecimalMin("0")
          @DecimalMax("100")
          Double maxScore,
      @Parameter(description = "Only zones whose field reports mention this need")
          @RequestParam(required = false)
          Need need) {
    var query = new GetZoneRankingQuery(new EmergencyId(emergencyId), minScore, maxScore, need);
    return zoneQueries.handle(query).stream().map(ZoneResourceAssembler::toRankingItem).toList();
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @GetMapping("/zones/in-charge")
  @Operation(
      summary = "List the zones in charge of the signed-in authority",
      description =
          "Zones of active emergencies it registered or received at a shift change, with their"
              + " last status (US-23). In registration order.")
  @ApiResponse(responseCode = "200", description = "Zones in charge")
  public List<ZoneResource> findInCharge(@AuthenticationPrincipal AuthenticatedUser user) {
    return zoneQueries.handle(new GetZonesInChargeQuery(new AuthorityId(user.userId()))).stream()
        .map(ZoneResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/zones/{zoneId}/history")
  @Operation(
      summary = "Get the history of decisions of a zone",
      description =
          "From the oldest to the most recent, each with the authority who took it and its"
              + " reason (US-20): registration, plan modifications, approvals and rejections,"
              + " manual priority, report corrections and shift changes. It is the same for every"
              + " authority, so the next responsible sees the decisions of the previous shift.")
  @ApiResponse(responseCode = "200", description = "History of the zone")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public List<ZoneDecisionResource> findHistory(@PathVariable UUID zoneId) {
    return zoneHistory.handle(new GetZoneHistoryQuery(new ZoneId(zoneId))).stream()
        .map(ZoneResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/zones/{zoneId}")
  @Operation(summary = "Get a zone")
  @ApiResponse(responseCode = "200", description = "Zone found")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ZoneResource findById(@PathVariable UUID zoneId) {
    return ZoneResourceAssembler.toResource(
        zoneQueries.handle(new GetZoneByIdQuery(new ZoneId(zoneId))));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PatchMapping("/zones/{zoneId}/population-impact")
  @Operation(
      summary = "Update the population impact of a zone",
      description =
          "Only the counts sent change; the others keep their value. Use it to add the"
              + " population of a zone registered without it. The urgency score is recalculated"
              + " and the response already includes it, unless the AI Service did not answer.")
  @ApiResponse(responseCode = "200", description = "Population impact updated")
  @ApiResponse(responseCode = "400", description = "Invalid request, for example a negative count")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  @ApiResponse(responseCode = "422", description = "Inconsistent population impact")
  public ZoneResource updatePopulationImpact(
      @PathVariable UUID zoneId, @Valid @RequestBody PopulationImpactResource request) {
    Zone zone =
        updatePopulationImpact.handle(ZoneResourceAssembler.toCommand(new ZoneId(zoneId), request));
    return ZoneResourceAssembler.toResource(zoneQueries.handle(new GetZoneByIdQuery(zone.id())));
  }

  @GetMapping("/zones/{zoneId}/score")
  @Operation(
      summary = "Get the urgency score of a zone with its explanation",
      description =
          "topFeatures are the variables that weighed the most (TS-02, US-05). Without a score,"
              + " available is false and missingVariables lists what the zone lacks.")
  @ApiResponse(responseCode = "200", description = "Score of the zone, or why it is not available")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ZoneScoreResource findScore(@PathVariable UUID zoneId) {
    return ZoneResourceAssembler.toScoreResource(
        zoneQueries.handle(new GetZoneByIdQuery(new ZoneId(zoneId))));
  }

  @GetMapping("/zones/{zoneId}/report")
  @Operation(
      summary = "Export the report of a zone as PDF",
      description =
          "Includes the impact of the zone, its urgency score with the points each variable"
              + " added and the model version, and its field reports (US-05). The document is in"
              + " the language of Accept-Language.")
  @ApiResponse(responseCode = "200", description = "PDF of the zone report")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ResponseEntity<byte[]> exportReport(@PathVariable UUID zoneId) {
    byte[] pdf =
        zoneReports.handle(
            new GetZoneReportQuery(new ZoneId(zoneId), LocaleContextHolder.getLocale()));
    var disposition =
        ContentDisposition.attachment().filename("zone-report-" + zoneId + ".pdf").build();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(pdf);
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping("/zones/{zoneId}/score")
  @Operation(
      summary = "Recalculate the urgency score of a zone",
      description =
          "The score is recalculated on its own when the zone or its reports change. Use this"
              + " endpoint when the AI Service did not answer at that moment.")
  @ApiResponse(responseCode = "200", description = "Score recalculated")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  @ApiResponse(responseCode = "422", description = "The zone has no population")
  @ApiResponse(responseCode = "503", description = "The AI Service is not available")
  public ZoneScoreResource calculateScore(@PathVariable UUID zoneId) {
    Zone zone = calculateUrgencyScore.handle(new CalculateUrgencyScoreCommand(new ZoneId(zoneId)));
    return ZoneResourceAssembler.toScoreResource(zone);
  }
}
