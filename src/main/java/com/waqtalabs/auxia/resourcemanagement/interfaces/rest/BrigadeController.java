package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AddStaffMemberCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ChangeBrigadeAvailabilityCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.CreateBrigadeCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.RemoveStaffMemberCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ChangeBrigadeAvailabilityCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.RemoveStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetBrigadeByIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetBrigadesByOrganizationIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.BrigadeQueryService;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.assemblers.BrigadeResourceAssembler;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.AddStaffMemberRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.BrigadeResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.ChangeAvailabilityRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.CreateBrigadeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brigades")
@Tag(name = "Brigades", description = "Field brigades that deliver the distribution plans")
public class BrigadeController {

  private final CreateBrigadeCommandHandler createBrigade;
  private final AddStaffMemberCommandHandler addStaffMember;
  private final RemoveStaffMemberCommandHandler removeStaffMember;
  private final ChangeBrigadeAvailabilityCommandHandler changeAvailability;
  private final BrigadeQueryService brigadeQueries;

  public BrigadeController(
      CreateBrigadeCommandHandler createBrigade,
      AddStaffMemberCommandHandler addStaffMember,
      RemoveStaffMemberCommandHandler removeStaffMember,
      ChangeBrigadeAvailabilityCommandHandler changeAvailability,
      BrigadeQueryService brigadeQueries) {
    this.createBrigade = createBrigade;
    this.addStaffMember = addStaffMember;
    this.removeStaffMember = removeStaffMember;
    this.changeAvailability = changeAvailability;
    this.brigadeQueries = brigadeQueries;
  }

  @PostMapping
  @Operation(
      summary = "Create a brigade",
      description = "It starts AVAILABLE and without members. Names are unique per organization.")
  @ApiResponse(responseCode = "201", description = "Brigade created")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "409", description = "The organization has a brigade with that name")
  public ResponseEntity<BrigadeResource> create(@Valid @RequestBody CreateBrigadeRequest request) {
    Brigade brigade = createBrigade.handle(BrigadeResourceAssembler.toCommand(request));
    return ResponseEntity.created(URI.create("/api/v1/brigades/" + brigade.id().value()))
        .body(BrigadeResourceAssembler.toResource(brigade));
  }

  @GetMapping
  @Operation(
      summary = "List the brigades of an organization",
      description =
          "Ordered by name. Until Identity Access exists, the organization is a query parameter.")
  @ApiResponse(responseCode = "200", description = "Brigades of the organization")
  public List<BrigadeResource> findByOrganization(
      @RequestParam UUID organizationId,
      @Parameter(description = "Only the brigades in this status")
          @RequestParam(required = false)
          BrigadeStatus status) {
    var query = new GetBrigadesByOrganizationIdQuery(new OrganizationId(organizationId), status);
    return brigadeQueries.handle(query).stream().map(BrigadeResourceAssembler::toResource).toList();
  }

  @GetMapping("/{brigadeId}")
  @Operation(summary = "Get a brigade with its members and its assignment in progress")
  @ApiResponse(responseCode = "200", description = "Brigade found")
  @ApiResponse(responseCode = "404", description = "The brigade does not exist")
  public BrigadeResource findById(@PathVariable UUID brigadeId) {
    return BrigadeResourceAssembler.toResource(
        brigadeQueries.handle(new GetBrigadeByIdQuery(new BrigadeId(brigadeId))));
  }

  @PostMapping("/{brigadeId}/members")
  @Operation(summary = "Add a member to a brigade")
  @ApiResponse(responseCode = "201", description = "Member added; the brigade is returned")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "404", description = "The brigade does not exist")
  public ResponseEntity<BrigadeResource> addMember(
      @PathVariable UUID brigadeId, @Valid @RequestBody AddStaffMemberRequest request) {
    Brigade brigade =
        addStaffMember.handle(BrigadeResourceAssembler.toCommand(brigadeId, request));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BrigadeResourceAssembler.toResource(brigade));
  }

  @DeleteMapping("/{brigadeId}/members/{memberId}")
  @Operation(
      summary = "Remove a member from a brigade",
      description = "An assigned brigade keeps at least one LEADER.")
  @ApiResponse(responseCode = "200", description = "Member removed; the brigade is returned")
  @ApiResponse(responseCode = "404", description = "The brigade or the member does not exist")
  @ApiResponse(responseCode = "422", description = "It is the last leader of an assigned brigade")
  public BrigadeResource removeMember(@PathVariable UUID brigadeId, @PathVariable UUID memberId) {
    var command =
        new RemoveStaffMemberCommand(new BrigadeId(brigadeId), new StaffMemberId(memberId));
    return BrigadeResourceAssembler.toResource(removeStaffMember.handle(command));
  }

  @PatchMapping("/{brigadeId}/availability")
  @Operation(
      summary = "Put a brigade on or off duty",
      description = "Only AVAILABLE or OFF_DUTY, and only without an assignment in progress.")
  @ApiResponse(responseCode = "200", description = "Availability changed")
  @ApiResponse(responseCode = "404", description = "The brigade does not exist")
  @ApiResponse(responseCode = "409", description = "The brigade has an assignment in progress")
  @ApiResponse(responseCode = "422", description = "The new status is ASSIGNED")
  public BrigadeResource changeAvailability(
      @PathVariable UUID brigadeId, @Valid @RequestBody ChangeAvailabilityRequest request) {
    var command = new ChangeBrigadeAvailabilityCommand(new BrigadeId(brigadeId), request.status());
    return BrigadeResourceAssembler.toResource(changeAvailability.handle(command));
  }
}
