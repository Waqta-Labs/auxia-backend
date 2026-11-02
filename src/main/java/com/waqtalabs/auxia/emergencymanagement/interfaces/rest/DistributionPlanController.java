package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.ApproveDistributionPlanCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AssignDistributionPlanBrigadeCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.GeneratedRecommendation;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.ModifyDistributionPlanCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RejectDistributionPlanCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.ApproveDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignDistributionPlanBrigadeCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.GenerateDistributionRecommendationCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RejectDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlanByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlansByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlansByZoneIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.DistributionPlanQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.DistributionPlanResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.BrigadeAssignmentResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.DistributionPlanResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.DistributionRecommendationResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ModifyDistributionPlanRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RejectDistributionPlanRequest;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Distribution plans of the organization of the signed-in user, which comes from the JWT. Any
 * user of the organization reads them; only an AUTHORITY generates, modifies and decides them. A
 * plan of another organization is not found.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Distribution plans",
    description = "Recommendation, modification, approval and rejection (US-11 to US-14)")
public class DistributionPlanController {

  private final GenerateDistributionRecommendationCommandHandler generateRecommendation;
  private final ModifyDistributionPlanCommandHandler modifyPlan;
  private final ApproveDistributionPlanCommandHandler approvePlan;
  private final RejectDistributionPlanCommandHandler rejectPlan;
  private final AssignDistributionPlanBrigadeCommandHandler assignBrigade;
  private final DistributionPlanQueryService planQueries;

  public DistributionPlanController(
      GenerateDistributionRecommendationCommandHandler generateRecommendation,
      ModifyDistributionPlanCommandHandler modifyPlan,
      ApproveDistributionPlanCommandHandler approvePlan,
      RejectDistributionPlanCommandHandler rejectPlan,
      AssignDistributionPlanBrigadeCommandHandler assignBrigade,
      DistributionPlanQueryService planQueries) {
    this.generateRecommendation = generateRecommendation;
    this.modifyPlan = modifyPlan;
    this.approvePlan = approvePlan;
    this.rejectPlan = rejectPlan;
    this.assignBrigade = assignBrigade;
    this.planQueries = planQueries;
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping("/emergencies/{emergencyId}/distribution-plans")
  @Operation(
      summary = "Recommend a distribution of the inventory among the prioritized zones",
      description =
          "Splits the available inventory of the organization among the zones of the emergency"
              + " with an urgency score and identified needs, without exceeding it. Proposes and"
              + " reserves one plan for each zone that receives resources, and lists the zones"
              + " left partially or totally unattended (US-11).")
  @ApiResponse(responseCode = "201", description = "Plans proposed and their stock reserved")
  @ApiResponse(responseCode = "200", description = "No zone receives resources; nothing proposed")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  @ApiResponse(
      responseCode = "409",
      description = "The emergency is closed, or has proposed plans still to decide")
  @ApiResponse(
      responseCode = "422",
      description = "No zone has a score and needs, or there is no available stock")
  @ApiResponse(responseCode = "503", description = "The AI Service is not available")
  public ResponseEntity<DistributionRecommendationResource> recommend(
      @PathVariable UUID emergencyId, @AuthenticationPrincipal AuthenticatedUser user) {
    GeneratedRecommendation recommendation =
        generateRecommendation.handle(
            new GenerateDistributionRecommendationCommand(
                new EmergencyId(emergencyId), authorityOf(user), organizationOf(user)));
    HttpStatus status = recommendation.plans().isEmpty() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status)
        .body(DistributionPlanResourceAssembler.toResource(recommendation));
  }

  @GetMapping("/emergencies/{emergencyId}/distribution-plans")
  @Operation(summary = "List the plans of an emergency", description = "From the most recent.")
  @ApiResponse(responseCode = "200", description = "Plans of the organization")
  @ApiResponse(responseCode = "404", description = "The emergency does not exist")
  public List<DistributionPlanResource> findByEmergency(
      @PathVariable UUID emergencyId,
      @Parameter(description = "Only the plans in this status")
          @RequestParam(required = false)
          DistributionPlanStatus status,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var query =
        new GetDistributionPlansByEmergencyIdQuery(
            new EmergencyId(emergencyId), organizationOf(user), status);
    return planQueries.handle(query).stream()
        .map(DistributionPlanResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/zones/{zoneId}/distribution-plans")
  @Operation(
      summary = "History of the plans of a zone",
      description =
          "Every plan of the zone with its decision, its modification and their justifications,"
              + " from the most recent (US-14).")
  @ApiResponse(responseCode = "200", description = "Plans of the zone")
  @ApiResponse(responseCode = "404", description = "The zone does not exist")
  public List<DistributionPlanResource> findByZone(
      @PathVariable UUID zoneId, @AuthenticationPrincipal AuthenticatedUser user) {
    var query = new GetDistributionPlansByZoneIdQuery(new ZoneId(zoneId), organizationOf(user));
    return planQueries.handle(query).stream()
        .map(DistributionPlanResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/distribution-plans/{planId}")
  @Operation(summary = "Get a plan with its recommended and current quantities")
  @ApiResponse(responseCode = "200", description = "Plan found")
  @ApiResponse(responseCode = "404", description = "Not a plan of the organization")
  public DistributionPlanResource findById(
      @PathVariable UUID planId, @AuthenticationPrincipal AuthenticatedUser user) {
    var query =
        new GetDistributionPlanByIdQuery(new DistributionPlanId(planId), organizationOf(user));
    return DistributionPlanResourceAssembler.toResource(planQueries.handle(query));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PatchMapping("/distribution-plans/{planId}/items")
  @Operation(
      summary = "Modify the quantities of a proposed plan",
      description =
          "Needs a reason (US-14). The recommended quantities are kept, so the original"
              + " recommendation survives (US-13). The reservation follows the new quantities,"
              + " which cannot exceed the available stock.")
  @ApiResponse(responseCode = "200", description = "Plan modified")
  @ApiResponse(responseCode = "400", description = "Invalid request, for example without reason")
  @ApiResponse(responseCode = "404", description = "Not a plan of the organization")
  @ApiResponse(
      responseCode = "409",
      description = "The plan is no longer proposed, or there is not enough stock")
  @ApiResponse(
      responseCode = "422",
      description = "A resource is repeated, or no resource is left to distribute")
  public DistributionPlanResource modify(
      @PathVariable UUID planId,
      @Valid @RequestBody ModifyDistributionPlanRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        DistributionPlanResourceAssembler.toCommand(
            planId, authorityOf(user), organizationOf(user), request);
    return DistributionPlanResourceAssembler.toResource(modifyPlan.handle(command));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping("/distribution-plans/{planId}/approve")
  @Operation(
      summary = "Approve a proposed plan",
      description =
          "The authority is the signed-in user, so the request has no body (US-12, TS-C01). The"
              + " stock stays reserved for the delivery.")
  @ApiResponse(responseCode = "200", description = "Plan approved")
  @ApiResponse(responseCode = "404", description = "Not a plan of the organization")
  @ApiResponse(
      responseCode = "409",
      description = "The plan is already approved, or it was rejected or executed")
  public DistributionPlanResource approve(
      @PathVariable UUID planId, @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new ApproveDistributionPlanCommand(
            new DistributionPlanId(planId), authorityOf(user), organizationOf(user));
    return DistributionPlanResourceAssembler.toResource(approvePlan.handle(command));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping("/distribution-plans/{planId}/reject")
  @Operation(
      summary = "Reject a proposed plan",
      description = "Needs a reason, which stays in the history of the zone (US-14).")
  @ApiResponse(responseCode = "200", description = "Plan rejected and its stock released")
  @ApiResponse(responseCode = "400", description = "The reason is missing")
  @ApiResponse(responseCode = "404", description = "Not a plan of the organization")
  @ApiResponse(responseCode = "409", description = "The plan is no longer proposed")
  public DistributionPlanResource reject(
      @PathVariable UUID planId,
      @Valid @RequestBody RejectDistributionPlanRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new RejectDistributionPlanCommand(
            new DistributionPlanId(planId),
            authorityOf(user),
            organizationOf(user),
            request.reason());
    return DistributionPlanResourceAssembler.toResource(rejectPlan.handle(command));
  }

  @PreAuthorize("hasRole('AUTHORITY')")
  @PostMapping("/distribution-plans/{planId}/brigade-assignment")
  @Operation(
      summary = "Assign a brigade to an approved plan",
      description =
          "The brigade is assigned on approval. When every brigade was busy then, or the AI"
              + " Service did not answer, the authority asks for one again here. The AI Service"
              + " suggests the available brigade closest to the zone.")
  @ApiResponse(responseCode = "201", description = "Brigade assigned")
  @ApiResponse(responseCode = "404", description = "Not a plan of the organization")
  @ApiResponse(
      responseCode = "409",
      description = "The plan is not approved, or a brigade already has it")
  @ApiResponse(
      responseCode = "422",
      description = "No brigade of the organization is available with a leader")
  @ApiResponse(responseCode = "503", description = "The AI Service is not available")
  @ResponseStatus(HttpStatus.CREATED)
  public BrigadeAssignmentResource assignBrigade(
      @PathVariable UUID planId, @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new AssignDistributionPlanBrigadeCommand(
            new DistributionPlanId(planId), organizationOf(user), Optional.of(authorityOf(user)));
    return new BrigadeAssignmentResource(planId, assignBrigade.handle(command).value());
  }

  private static AuthorityId authorityOf(AuthenticatedUser user) {
    return new AuthorityId(user.userId());
  }

  private static OrganizationId organizationOf(AuthenticatedUser user) {
    return new OrganizationId(user.organizationId());
  }
}
