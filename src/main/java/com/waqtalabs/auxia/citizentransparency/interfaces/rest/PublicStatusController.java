package com.waqtalabs.auxia.citizentransparency.interfaces.rest;

import com.waqtalabs.auxia.citizentransparency.application.queries.GetPublicEmergenciesQuery;
import com.waqtalabs.auxia.citizentransparency.application.queries.GetPublicZoneByIdQuery;
import com.waqtalabs.auxia.citizentransparency.application.queries.GetPublicZonesByEmergencyIdQuery;
import com.waqtalabs.auxia.citizentransparency.application.queries.SearchPublicZonesQuery;
import com.waqtalabs.auxia.citizentransparency.application.queryservices.PublicStatusQueryService;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.assemblers.PublicStatusResourceAssembler;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources.PublicEmergencyResource;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources.PublicZoneStatusResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public view for citizens (EP-08): no sign-in. Answers may be cached for 60 seconds, so a
 * change of stage can take up to a minute to show.
 */
@RestController
@RequestMapping("/api/v1/public")
@SecurityRequirements
@Tag(name = "Public status", description = "Stage of attention of each zone, for citizens (EP-08)")
public class PublicStatusController {

  private final PublicStatusQueryService queries;

  public PublicStatusController(PublicStatusQueryService queries) {
    this.queries = queries;
  }

  @GetMapping("/emergencies")
  @Operation(summary = "List the emergencies", description = "The most recent first.")
  public List<PublicEmergencyResource> emergencies() {
    return queries.handle(new GetPublicEmergenciesQuery()).stream()
        .map(PublicStatusResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/emergencies/{emergencyId}/zones")
  @Operation(summary = "List the zones of an emergency with their stage", description = "By name.")
  @ApiResponse(responseCode = "200", description = "Zones of the emergency")
  @ApiResponse(responseCode = "404", description = "No information about the emergency")
  public List<PublicZoneStatusResource> zonesOfEmergency(@PathVariable UUID emergencyId) {
    var query = new GetPublicZonesByEmergencyIdQuery(new EmergencyId(emergencyId));
    return queries.handle(query).stream().map(PublicStatusResourceAssembler::toResource).toList();
  }

  @GetMapping("/zones/{zoneId}")
  @Operation(
      summary = "Stage of attention of a zone",
      description =
          "The current stage and the delivery history: how many, the last one and whether it is"
              + " verified (US-24, US-25).")
  @ApiResponse(responseCode = "200", description = "Zone found")
  @ApiResponse(responseCode = "404", description = "The zone is not registered yet")
  public PublicZoneStatusResource zone(@PathVariable UUID zoneId) {
    return PublicStatusResourceAssembler.toResource(
        queries.handle(new GetPublicZoneByIdQuery(new ZoneId(zoneId))));
  }

  @GetMapping("/zones")
  @Operation(summary = "Find the zones of a district", description = "By name.")
  @ApiResponse(responseCode = "200", description = "Zones found; empty when none matches")
  @ApiResponse(responseCode = "400", description = "The district is missing")
  public List<PublicZoneStatusResource> search(
      @Parameter(description = "District, ignoring case", example = "Lurigancho-Chosica")
          @RequestParam
          String district,
      @Parameter(description = "Part of the name of the zone, ignoring case")
          @RequestParam(required = false)
          String name) {
    return queries.handle(new SearchPublicZonesQuery(district, name)).stream()
        .map(PublicStatusResourceAssembler::toResource)
        .toList();
  }
}
