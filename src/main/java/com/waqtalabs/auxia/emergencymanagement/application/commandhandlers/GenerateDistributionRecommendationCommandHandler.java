package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.GenerateDistributionRecommendationCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.InventoryAvailabilityPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.ResourceReservationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanProposed;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyClosedForDistributionException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoAvailableStockException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoPrioritizedZonesException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.PendingDistributionPlansException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.services.DistributionOptimizationService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionRecommendation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PrioritizedZone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneAllocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Splits the available inventory of the organization of the authority among the prioritized
 * zones of an emergency (US-11). It asks DistributionOptimizationService for one recommendation
 * across every zone with an urgency score and identified needs, proposes one plan for each zone
 * that receives something, and reserves its stock, so two plans never count on the same units.
 * Everything happens in one transaction: if a reservation fails, no plan is proposed.
 */
@Service
public class GenerateDistributionRecommendationCommandHandler {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final DistributionPlanRepository distributionPlanRepository;
  private final DistributionOptimizationService optimizationService;
  private final InventoryAvailabilityPort inventoryAvailability;
  private final ResourceReservationPort resourceReservation;
  private final AuthorizationPort authorization;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public GenerateDistributionRecommendationCommandHandler(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      DistributionPlanRepository distributionPlanRepository,
      DistributionOptimizationService optimizationService,
      InventoryAvailabilityPort inventoryAvailability,
      ResourceReservationPort resourceReservation,
      AuthorizationPort authorization,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.distributionPlanRepository = distributionPlanRepository;
    this.optimizationService = optimizationService;
    this.inventoryAvailability = inventoryAvailability;
    this.resourceReservation = resourceReservation;
    this.authorization = authorization;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  /**
   * @throws EmergencyClosedForDistributionException if the emergency is closed
   * @throws PendingDistributionPlansException if the organization has proposed plans of the
   *     emergency still to decide
   * @throws NoPrioritizedZonesException if no zone has a score and identified needs
   * @throws NoAvailableStockException if the inventory has nothing available
   */
  @Transactional
  public GeneratedRecommendation handle(GenerateDistributionRecommendationCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    Emergency emergency =
        emergencyRepository
            .findById(command.emergencyId())
            .orElseThrow(() -> new EmergencyNotFoundException(command.emergencyId()));
    if (!emergency.isActive()) {
      throw new EmergencyClosedForDistributionException(emergency.id());
    }
    if (distributionPlanRepository.hasPendingPlans(emergency.id(), command.organizationId())) {
      throw new PendingDistributionPlansException(emergency.id());
    }

    Map<ZoneId, Zone> prioritized = new LinkedHashMap<>();
    List<PrioritizedZone> zones = new ArrayList<>();
    List<ExcludedZone> excluded = new ArrayList<>();
    for (Zone zone : zoneRepository.findByEmergencyId(emergency.id())) {
      Optional<PrioritizedZone> candidate = prioritize(zone);
      if (candidate.isEmpty()) {
        ExclusionReason reason =
            zone.urgencyScore().isEmpty() ? ExclusionReason.NO_SCORE : ExclusionReason.NO_NEEDS;
        excluded.add(new ExcludedZone(zone, reason));
      } else {
        prioritized.put(zone.id(), zone);
        zones.add(candidate.get());
      }
    }
    if (zones.isEmpty()) {
      throw new NoPrioritizedZonesException(emergency.id());
    }
    List<AvailableResource> stock =
        inventoryAvailability.availableStock(command.organizationId()).stream()
            .filter(resource -> resource.available() > 0)
            .toList();
    if (stock.isEmpty()) {
      throw new NoAvailableStockException(emergency.id());
    }

    DistributionRecommendation recommendation = optimizationService.recommend(zones, stock);
    LocalDateTime now = LocalDateTime.now(clock);
    List<DistributionPlan> plans = new ArrayList<>();
    List<UnattendedZone> unattended = new ArrayList<>();
    for (ZoneAllocation allocation : recommendation.allocations()) {
      Zone zone = prioritized.get(allocation.zoneId());
      if (zone == null) {
        continue;
      }
      if (allocation.coverage() != Coverage.FULL) {
        unattended.add(new UnattendedZone(zone, allocation.coverage()));
      }
      if (allocation.receivesResources()) {
        plans.add(propose(emergency, zone, allocation, recommendation, command, now));
      }
    }
    return new GeneratedRecommendation(
        plans, unattended, excluded, recommendation.justification(), recommendation.modelVersion());
  }

  /** The zone as the recommendation receives it, or empty without a score or needs. */
  private Optional<PrioritizedZone> prioritize(Zone zone) {
    if (zone.urgencyScore().isEmpty() || !zone.hasSufficientDataForScoring()) {
      return Optional.empty();
    }
    List<StructuredFieldData> reports =
        fieldReportRepository.findByZoneId(zone.id()).stream()
            .map(FieldReport::structuredData)
            .flatMap(Optional::stream)
            .toList();
    UrgencyIndicators indicators = zone.urgencyIndicators(reports);
    if (indicators.needs().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new PrioritizedZone(
            zone.id(),
            zone.urgencyScore().get().value(),
            indicators.peopleAffected(),
            indicators.needs()));
  }

  private DistributionPlan propose(
      Emergency emergency,
      Zone zone,
      ZoneAllocation allocation,
      DistributionRecommendation recommendation,
      GenerateDistributionRecommendationCommand command,
      LocalDateTime now) {
    DistributionPlan plan =
        distributionPlanRepository.save(
            DistributionPlan.propose(
                emergency.id(),
                zone.id(),
                command.organizationId(),
                allocation.items(),
                allocation.coverage(),
                recommendation.justification(),
                recommendation.modelVersion(),
                now));
    resourceReservation.reserve(
        command.organizationId(), plan.id(), plan.distributedQuantities());
    eventPublisher.publish(new DistributionPlanProposed(plan.id(), emergency.id(), zone.id()));
    return plan;
  }

  /** Why a zone of the emergency was left out of the recommendation. */
  public enum ExclusionReason {
    /** The zone has no urgency score yet, so it is not prioritized. */
    NO_SCORE,
    /** No processed field report of the zone mentions a need. */
    NO_NEEDS
  }

  /** A zone left out of the recommendation. */
  public record ExcludedZone(Zone zone, ExclusionReason reason) {}

  /** A zone that the recommendation covers only in part, or not at all (US-11, scenario 2). */
  public record UnattendedZone(Zone zone, Coverage coverage) {}

  /** The proposed plans and what the recommendation could not cover. */
  public record GeneratedRecommendation(
      List<DistributionPlan> plans,
      List<UnattendedZone> unattendedZones,
      List<ExcludedZone> excludedZones,
      String justification,
      String modelVersion) {}
}
