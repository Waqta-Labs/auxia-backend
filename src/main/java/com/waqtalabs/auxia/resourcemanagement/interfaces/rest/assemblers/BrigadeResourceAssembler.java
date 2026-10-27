package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AddStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.CreateBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.AddStaffMemberRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.BrigadeAssignmentResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.BrigadeResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.CreateBrigadeRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.LocationResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.StaffMemberResource;
import java.util.Comparator;
import java.util.UUID;

public final class BrigadeResourceAssembler {

  private BrigadeResourceAssembler() {}

  public static CreateBrigadeCommand toCommand(
      OrganizationId organizationId, CreateBrigadeRequest request) {
    LocationResource base = request.baseLocation();
    return new CreateBrigadeCommand(
        organizationId,
        request.name(),
        new Location(base.latitude(), base.longitude()));
  }

  public static AddStaffMemberCommand toCommand(
      UUID brigadeId, OrganizationId organizationId, AddStaffMemberRequest request) {
    return new AddStaffMemberCommand(
        new BrigadeId(brigadeId),
        organizationId,
        request.fullName(),
        request.role(),
        request.phone());
  }

  public static BrigadeResource toResource(Brigade brigade) {
    Location base = brigade.baseLocation();
    return new BrigadeResource(
        brigade.id().value(),
        brigade.organizationId().value(),
        brigade.name(),
        new LocationResource(base.latitude(), base.longitude()),
        brigade.status(),
        brigade.hasLeader(),
        brigade.members().stream()
            .sorted(Comparator.comparing(StaffMember::fullName, String.CASE_INSENSITIVE_ORDER))
            .map(
                member ->
                    new StaffMemberResource(
                        member.id().value(),
                        member.fullName(),
                        member.role(),
                        member.phone().orElse(null)))
            .toList(),
        brigade
            .currentAssignment()
            .map(
                assignment ->
                    new BrigadeAssignmentResource(
                        assignment.distributionPlanId().value(),
                        assignment.zoneId().value(),
                        assignment.assignedAt()))
            .orElse(null));
  }
}
