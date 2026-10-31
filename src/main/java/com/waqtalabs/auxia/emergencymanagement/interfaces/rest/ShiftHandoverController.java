package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.HandOverShiftCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.HandOverShiftCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ShiftHandover;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.HandOverShiftRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ShiftHandoverResource;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shift-handovers")
@Tag(name = "Shift handovers", description = "Shift changes between authorities")
public class ShiftHandoverController {

  private final HandOverShiftCommandHandler handOverShift;

  public ShiftHandoverController(HandOverShiftCommandHandler handOverShift) {
    this.handOverShift = handOverShift;
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Hand over the shift to the next authority",
      description =
          "The zones of active emergencies the signed-in authority follows pass to the next one,"
              + " who sees them in GET /zones/in-charge (US-23). The change is added to the"
              + " history of each zone.")
  @ApiResponse(responseCode = "201", description = "Shift handed over, with the zones that passed")
  @ApiResponse(responseCode = "400", description = "The next authority is missing")
  @ApiResponse(
      responseCode = "422",
      description =
          "The next user is not an active authority of the organization, or is the same one")
  public ShiftHandoverResource handOver(
      @Valid @RequestBody HandOverShiftRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new HandOverShiftCommand(
            new OrganizationId(user.organizationId()),
            new AuthorityId(user.userId()),
            new AuthorityId(request.incomingAuthorityId()),
            request.notes());
    ShiftHandover handover = handOverShift.handle(command);
    return new ShiftHandoverResource(
        handover.id().value(),
        handover.outgoing().value(),
        handover.incoming().value(),
        handover.handedOverAt(),
        handover.notes().orElse(null),
        handover.zoneIds().stream().map(ZoneId::value).toList());
  }
}
