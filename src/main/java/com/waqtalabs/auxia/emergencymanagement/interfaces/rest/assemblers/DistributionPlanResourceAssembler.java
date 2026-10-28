package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.GeneratedRecommendation;
import com.waqtalabs.auxia.emergencymanagement.application.commands.ModifyDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.DistributionItemResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.DistributionPlanResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.DistributionRecommendationResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ExcludedZoneResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ModifyDistributionPlanRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.PlanDecisionResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.PlanModificationResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.UnattendedZoneResource;
import java.util.UUID;

public final class DistributionPlanResourceAssembler {

  private DistributionPlanResourceAssembler() {}

  public static ModifyDistributionPlanCommand toCommand(
      UUID planId,
      AuthorityId authority,
      OrganizationId organization,
      ModifyDistributionPlanRequest request) {
    return new ModifyDistributionPlanCommand(
        new DistributionPlanId(planId),
        authority,
        organization,
        request.items().stream()
            .map(
                item -> new ResourceQuantity(new ResourceId(item.resourceId()), item.quantity()))
            .toList(),
        request.reason());
  }

  public static DistributionRecommendationResource toResource(
      GeneratedRecommendation recommendation) {
    return new DistributionRecommendationResource(
        recommendation.plans().stream().map(DistributionPlanResourceAssembler::toResource).toList(),
        recommendation.unattendedZones().stream()
            .map(
                unattended ->
                    new UnattendedZoneResource(
                        unattended.zone().id().value(),
                        unattended.zone().name(),
                        unattended.coverage()))
            .toList(),
        recommendation.excludedZones().stream()
            .map(
                excluded ->
                    new ExcludedZoneResource(
                        excluded.zone().id().value(), excluded.zone().name(), excluded.reason()))
            .toList(),
        recommendation.justification(),
        recommendation.modelVersion());
  }

  public static DistributionPlanResource toResource(DistributionPlan plan) {
    return new DistributionPlanResource(
        plan.id().value(),
        plan.emergencyId().value(),
        plan.zoneId().value(),
        plan.organizationId().value(),
        plan.status(),
        plan.coverage(),
        plan.justification(),
        plan.modelVersion(),
        plan.createdAt(),
        plan.items().stream()
            .map(
                item ->
                    new DistributionItemResource(
                        item.resourceId().value(),
                        item.recommendedQuantity().orElse(null),
                        item.quantity()))
            .toList(),
        plan.modification()
            .map(
                modification ->
                    new PlanModificationResource(
                        modification.modifiedBy().value(),
                        modification.modifiedAt(),
                        modification.reason()))
            .orElse(null),
        plan.decision()
            .map(
                decision ->
                    new PlanDecisionResource(decision.decidedBy().value(), decision.decidedAt()))
            .orElse(null),
        plan.rejectedReason().orElse(null));
  }
}
