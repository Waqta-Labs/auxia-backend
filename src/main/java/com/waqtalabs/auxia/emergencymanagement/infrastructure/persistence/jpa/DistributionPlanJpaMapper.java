package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionItem;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionItemId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanModification;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DistributionPlanJpaMapper {

  private DistributionPlanJpaMapper() {}

  static DistributionPlan toDomain(DistributionPlanJpaEntity entity) {
    return DistributionPlan.reconstitute(
        new DistributionPlanId(entity.id),
        new EmergencyId(entity.emergencyId),
        new ZoneId(entity.zoneId),
        new OrganizationId(entity.organizationId),
        entity.items.stream().map(DistributionPlanJpaMapper::toDomain).toList(),
        entity.coverage,
        entity.justification,
        entity.modelVersion,
        entity.createdAt,
        entity.status,
        entity.modifiedBy == null
            ? null
            : new PlanModification(
                new AuthorityId(entity.modifiedBy), entity.modifiedAt, entity.modificationReason),
        entity.decidedBy == null
            ? null
            : new PlanDecision(new AuthorityId(entity.decidedBy), entity.decidedAt),
        entity.rejectedReason);
  }

  /**
   * Copies the aggregate into a new or managed entity. Lines are matched by id, so a modification
   * updates the existing rows, and a line the aggregate no longer has is deleted.
   */
  static void copy(DistributionPlan plan, DistributionPlanJpaEntity entity) {
    entity.id = plan.id().value();
    entity.emergencyId = plan.emergencyId().value();
    entity.zoneId = plan.zoneId().value();
    entity.organizationId = plan.organizationId().value();
    entity.status = plan.status();
    entity.coverage = plan.coverage();
    entity.justification = plan.justification();
    entity.modelVersion = plan.modelVersion();
    entity.createdAt = plan.createdAt();
    entity.decidedBy = plan.decision().map(decision -> decision.decidedBy().value()).orElse(null);
    entity.decidedAt = plan.decision().map(PlanDecision::decidedAt).orElse(null);
    entity.rejectedReason = plan.rejectedReason().orElse(null);
    entity.modifiedBy =
        plan.modification().map(modification -> modification.modifiedBy().value()).orElse(null);
    entity.modifiedAt = plan.modification().map(PlanModification::modifiedAt).orElse(null);
    entity.modificationReason = plan.modification().map(PlanModification::reason).orElse(null);

    Set<UUID> current =
        plan.items().stream().map(item -> item.id().value()).collect(Collectors.toSet());
    entity.items.removeIf(row -> !current.contains(row.id));
    Map<UUID, DistributionItemJpaEntity> rows =
        entity.items.stream().collect(Collectors.toMap(row -> row.id, Function.identity()));
    for (DistributionItem item : plan.items()) {
      DistributionItemJpaEntity row = rows.get(item.id().value());
      if (row == null) {
        row = new DistributionItemJpaEntity();
        row.id = item.id().value();
        row.plan = entity;
        row.resourceId = item.resourceId().value();
        row.recommendedQuantity = item.recommendedQuantity().orElse(null);
        entity.items.add(row);
      }
      row.quantity = item.quantity();
    }
  }

  private static DistributionItem toDomain(DistributionItemJpaEntity row) {
    return DistributionItem.reconstitute(
        new DistributionItemId(row.id),
        new ResourceId(row.resourceId),
        row.recommendedQuantity,
        row.quantity);
  }
}
