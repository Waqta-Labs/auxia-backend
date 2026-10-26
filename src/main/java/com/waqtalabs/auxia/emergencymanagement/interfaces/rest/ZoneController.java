package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZonesByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.ZoneResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterZoneRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Zones", description = "Areas affected by an emergency and their impact")
public class ZoneController {

  private final RegisterZoneCommandHandler registerZone;
  private final ZoneQueryService zoneQueries;

  public ZoneController(RegisterZoneCommandHandler registerZone, ZoneQueryService zoneQueries) {
    this.registerZone = registerZone;
    this.zoneQueries = zoneQueries;
  }

  @PostMapping("/emergencies/{emergencyId}/zones")
  @Operation(summary = "Register a zone of an emergency", description = "It starts REGISTERED.")
  @ApiResponse(responseCode = "201", description = "Zone registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  @ApiResponse(responseCode = "409", description = "The emergency is closed")
  @ApiResponse(responseCode = "422", description = "Inconsistent population impact")
  public ResponseEntity<ZoneResource> register(
      @PathVariable UUID emergencyId, @Valid @RequestBody RegisterZoneRequest request) {
    Zone zone = registerZone.handle(ZoneResourceAssembler.toCommand(emergencyId, request));
    return ResponseEntity.created(URI.create("/api/v1/zones/" + zone.id().value()))
        .body(ZoneResourceAssembler.toResource(zone));
  }

  @GetMapping("/emergencies/{emergencyId}/zones")
  @Operation(
      summary = "List the zones of an emergency",
      description =
          "Ranking from the most to the least urgent. Until the urgency score is calculated,"
              + " zones keep their registration order.")
  @ApiResponse(responseCode = "200", description = "Zones of the emergency")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  public List<ZoneResource> findByEmergency(@PathVariable UUID emergencyId) {
    var query = new GetZonesByEmergencyIdQuery(new EmergencyId(emergencyId));
    return zoneQueries.handle(query).stream().map(ZoneResourceAssembler::toResource).toList();
  }

  @GetMapping("/zones/{zoneId}")
  @Operation(summary = "Get a zone")
  @ApiResponse(responseCode = "200", description = "Zone found")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public ZoneResource findById(@PathVariable UUID zoneId) {
    return ZoneResourceAssembler.toResource(
        zoneQueries.handle(new GetZoneByIdQuery(new ZoneId(zoneId))));
  }
}
