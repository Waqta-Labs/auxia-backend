package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.CalculateUrgencyScoreCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.CalculateUrgencyScoreCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneRankingQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.ZoneResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterZoneRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneRankingItemResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneScoreResource;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Zones", description = "Areas affected by an emergency, their impact and urgency")
public class ZoneController {

  private final RegisterZoneCommandHandler registerZone;
  private final CalculateUrgencyScoreCommandHandler calculateUrgencyScore;
  private final ZoneQueryService zoneQueries;

  public ZoneController(
      RegisterZoneCommandHandler registerZone,
      CalculateUrgencyScoreCommandHandler calculateUrgencyScore,
      ZoneQueryService zoneQueries) {
    this.registerZone = registerZone;
    this.calculateUrgencyScore = calculateUrgencyScore;
    this.zoneQueries = zoneQueries;
  }

  @PostMapping("/emergencies/{emergencyId}/zones")
  @Operation(
      summary = "Register a zone of an emergency",
      description =
          "It starts REGISTERED. With its population, the urgency score is calculated right away"
              + " and the response already includes it, unless the AI Service did not answer.")
  @ApiResponse(responseCode = "201", description = "Zone registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  @ApiResponse(responseCode = "409", description = "The emergency is closed")
  @ApiResponse(responseCode = "422", description = "Inconsistent population impact")
  public ResponseEntity<ZoneResource> register(
      @PathVariable UUID emergencyId, @Valid @RequestBody RegisterZoneRequest request) {
    Zone zone = registerZone.handle(ZoneResourceAssembler.toCommand(emergencyId, request));
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

  @GetMapping("/zones/{zoneId}")
  @Operation(summary = "Get a zone")
  @ApiResponse(responseCode = "200", description = "Zone found")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ZoneResource findById(@PathVariable UUID zoneId) {
    return ZoneResourceAssembler.toResource(
        zoneQueries.handle(new GetZoneByIdQuery(new ZoneId(zoneId))));
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
