package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.BrigadeAssignment;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class BrigadeJpaMapper {

  private BrigadeJpaMapper() {}

  static Brigade toDomain(BrigadeJpaEntity entity) {
    return Brigade.reconstitute(
        new BrigadeId(entity.id),
        new OrganizationId(entity.organizationId),
        entity.name,
        new Location(entity.baseLatitude, entity.baseLongitude),
        entity.status,
        entity.members.stream()
            .map(
                row ->
                    StaffMember.reconstitute(
                        new StaffMemberId(row.id), row.fullName, row.role, row.phone))
            .toList(),
        entity.assignments.stream()
            .map(
                row ->
                    BrigadeAssignment.reconstitute(
                        new AssignmentId(row.id),
                        new DistributionPlanId(row.distributionPlanId),
                        new ZoneId(row.zoneId),
                        row.status,
                        row.assignedAt,
                        row.completedAt))
            .toList());
  }

  /**
   * Copies the aggregate into a new or managed entity. Members and assignments are matched by id:
   * existing rows are updated, new ones inserted and removed members deleted.
   */
  static void copy(Brigade brigade, BrigadeJpaEntity entity) {
    entity.id = brigade.id().value();
    entity.organizationId = brigade.organizationId().value();
    entity.name = brigade.name();
    entity.baseLatitude = brigade.baseLocation().latitude();
    entity.baseLongitude = brigade.baseLocation().longitude();
    entity.status = brigade.status();

    Set<UUID> memberIds =
        brigade.members().stream().map(member -> member.id().value()).collect(Collectors.toSet());
    entity.members.removeIf(row -> !memberIds.contains(row.id));
    Map<UUID, StaffMemberJpaEntity> members =
        entity.members.stream().collect(Collectors.toMap(row -> row.id, Function.identity()));
    for (StaffMember member : brigade.members()) {
      StaffMemberJpaEntity row = members.get(member.id().value());
      if (row == null) {
        row = new StaffMemberJpaEntity();
        row.id = member.id().value();
        row.brigade = entity;
        entity.members.add(row);
      }
      row.fullName = member.fullName();
      row.role = member.role();
      row.phone = member.phone().orElse(null);
    }

    Map<UUID, BrigadeAssignmentJpaEntity> assignments =
        entity.assignments.stream().collect(Collectors.toMap(row -> row.id, Function.identity()));
    for (BrigadeAssignment assignment : brigade.assignments()) {
      BrigadeAssignmentJpaEntity row = assignments.get(assignment.id().value());
      if (row == null) {
        row = new BrigadeAssignmentJpaEntity();
        row.id = assignment.id().value();
        row.brigade = entity;
        row.distributionPlanId = assignment.distributionPlanId().value();
        row.zoneId = assignment.zoneId().value();
        row.assignedAt = assignment.assignedAt();
        entity.assignments.add(row);
      }
      row.status = assignment.status();
      row.completedAt = assignment.completedAt().orElse(null);
    }
  }
}
