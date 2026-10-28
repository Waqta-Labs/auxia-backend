package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionItem;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JpaDistributionPlanRepositoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);

  @Autowired private EmergencyRepository emergencies;
  @Autowired private ZoneRepository zones;
  @Autowired private DistributionPlanRepository plans;
  @Autowired private EntityManager entityManager;

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final AuthorityId authority = new AuthorityId(UUID.randomUUID());
  private final ResourceId water = new ResourceId(UUID.randomUUID());
  private final ResourceId food = new ResourceId(UUID.randomUUID());
  private final ResourceId tents = new ResourceId(UUID.randomUUID());
  private Emergency emergency;
  private Zone zone;

  @BeforeEach
  void emergencyWithAZone() {
    emergency =
        emergencies.save(
            Emergency.register(
                "Huaico Chosica 2026",
                EmergencyType.LANDSLIDE,
                null,
                new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
                LocalDate.of(2026, 3, 14)));
    zone =
        zones.save(
            emergency.registerZone(
                "Sector Alto Perú",
                null,
                new PopulationImpact(600, 35, 140, 40, 15, 6),
                DamageLevel.SEVERE));
  }

  @Test
  void proposedPlanIsStoredWithItsRecommendedLines() {
    DistributionPlan plan = plans.save(propose(zone.id()));
    clear();

    DistributionPlan found = plans.findById(plan.id()).orElseThrow();
    assertThat(found.status()).isEqualTo(DistributionPlanStatus.PROPOSED);
    assertThat(found.organizationId()).isEqualTo(organization);
    assertThat(found.coverage()).isEqualTo(Coverage.PARTIAL);
    assertThat(found.justification()).isEqualTo("Proporcional al score");
    assertThat(found.modelVersion()).isEqualTo("rules-v1");
    assertThat(found.createdAt()).isEqualTo(NOW);
    assertThat(found.items())
        .extracting(
            DistributionItem::resourceId,
            item -> item.recommendedQuantity().orElse(null),
            DistributionItem::quantity)
        .containsExactlyInAnyOrder(tuple(water, 329, 329), tuple(food, 140, 140));
    assertThat(found.modification()).isEmpty();
    assertThat(found.decision()).isEmpty();
  }

  @Test
  void modifiedAndApprovedPlanKeepsTheOriginalRecommendation() {
    DistributionPlan plan = plans.save(propose(zone.id()));
    clear();
    DistributionPlan stored = plans.findById(plan.id()).orElseThrow();
    stored.modifyItems(
        List.of(
            new ResourceQuantity(water, 200),
            new ResourceQuantity(food, 0),
            new ResourceQuantity(tents, 10)),
        authority,
        "La zona recibió agua de otra ONG",
        NOW.plusMinutes(10));
    stored.approve(authority, NOW.plusMinutes(20));
    plans.save(stored);
    clear();

    DistributionPlan approved = plans.findById(plan.id()).orElseThrow();
    assertThat(approved.status()).isEqualTo(DistributionPlanStatus.APPROVED);
    assertThat(approved.decision()).contains(new PlanDecision(authority, NOW.plusMinutes(20)));
    assertThat(approved.modification().orElseThrow().reason())
        .isEqualTo("La zona recibió agua de otra ONG");
    assertThat(approved.items())
        .extracting(
            DistributionItem::resourceId,
            item -> item.recommendedQuantity().orElse(null),
            DistributionItem::quantity)
        .containsExactlyInAnyOrder(
            tuple(water, 329, 200), tuple(food, 140, 0), tuple(tents, null, 10));
  }

  @Test
  void rejectedPlanKeepsItsReason() {
    DistributionPlan plan = propose(zone.id());
    plan.reject(authority, "La vía está bloqueada", NOW);
    plans.save(plan);
    clear();

    DistributionPlan rejected = plans.findById(plan.id()).orElseThrow();
    assertThat(rejected.status()).isEqualTo(DistributionPlanStatus.REJECTED);
    assertThat(rejected.rejectedReason()).contains("La vía está bloqueada");
    assertThat(rejected.decision()).contains(new PlanDecision(authority, NOW));
  }

  @Test
  void plansAreListedByZoneAndEmergencyOfTheOrganizationFromTheMostRecent() {
    Zone other =
        zones.save(
            emergency.registerZone(
                "Quebrada Pedregal",
                null,
                new PopulationImpact(300, 10, 40, 5, 3, 1),
                DamageLevel.MODERATE));
    DistributionPlan older = plans.save(propose(zone.id(), NOW));
    DistributionPlan newer = plans.save(propose(zone.id(), NOW.plusHours(1)));
    DistributionPlan inOtherZone = plans.save(propose(other.id(), NOW));
    plans.save(
        DistributionPlan.propose(
            emergency.id(),
            zone.id(),
            new OrganizationId(UUID.randomUUID()),
            List.of(new ResourceQuantity(water, 1)),
            Coverage.FULL,
            "Otra organización",
            "rules-v1",
            NOW));
    clear();

    assertThat(plans.findByZoneId(zone.id(), organization))
        .extracting(DistributionPlan::id)
        .containsExactly(newer.id(), older.id());
    assertThat(plans.findByEmergencyId(emergency.id(), organization))
        .extracting(DistributionPlan::id)
        .containsExactlyInAnyOrder(newer.id(), older.id(), inOtherZone.id());
  }

  @Test
  void onlyProposedPlansArePending() {
    assertThat(plans.hasPendingPlans(emergency.id(), organization)).isFalse();
    DistributionPlan plan = plans.save(propose(zone.id()));

    assertThat(plans.hasPendingPlans(emergency.id(), organization)).isTrue();
    assertThat(plans.hasPendingPlans(emergency.id(), new OrganizationId(UUID.randomUUID())))
        .isFalse();

    plan.reject(authority, "No hace falta", NOW);
    plans.save(plan);
    assertThat(plans.hasPendingPlans(emergency.id(), organization)).isFalse();
  }

  @Test
  void planOfAZoneOfAnotherEmergencyIsRejectedByTheDatabase() {
    Emergency other =
        emergencies.save(
            Emergency.register(
                "Lluvias Piura 2026",
                EmergencyType.FLOOD,
                null,
                new AdministrativeLocation("Piura", "Piura", "Castilla"),
                LocalDate.of(2026, 2, 1)));
    DistributionPlan plan =
        DistributionPlan.propose(
            other.id(),
            zone.id(),
            organization,
            List.of(new ResourceQuantity(water, 1)),
            Coverage.FULL,
            "Zona de otra emergencia",
            "rules-v1",
            NOW);

    assertThatThrownBy(
            () -> {
              plans.save(plan);
              entityManager.flush();
            })
        .hasMessageContaining("fk_distribution_plan_zone");
  }

  private DistributionPlan propose(ZoneId zoneId) {
    return propose(zoneId, NOW);
  }

  private DistributionPlan propose(ZoneId zoneId, LocalDateTime at) {
    return DistributionPlan.propose(
        emergency.id(),
        zoneId,
        organization,
        List.of(new ResourceQuantity(water, 329), new ResourceQuantity(food, 140)),
        Coverage.PARTIAL,
        "Proporcional al score",
        "rules-v1",
        at);
  }

  private void clear() {
    entityManager.flush();
    entityManager.clear();
  }
}
