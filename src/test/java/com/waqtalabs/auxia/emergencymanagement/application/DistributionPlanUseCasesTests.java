package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.ApproveDistributionPlanCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AssignDistributionPlanBrigadeCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.ExcludedZone;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.UnattendedZone;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.ExclusionReason;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.GeneratedRecommendation;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.ModifyDistributionPlanCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RejectDistributionPlanCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.ApproveDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignDistributionPlanBrigadeCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.GenerateDistributionRecommendationCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.ModifyDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RejectDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlanByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlansByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlansByZoneIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.DistributionPlanQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanProposed;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanRejected;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyClosedForDistributionException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoAvailableBrigadeException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoAvailableStockException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoPrioritizedZonesException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.PendingDistributionPlansException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PrioritizedZone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.shared.application.exceptions.ActionNotAllowedException;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DistributionPlanUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-14T14:30:00Z"), ZoneOffset.ofHours(-5));
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final AuthorityId authority = new AuthorityId(UUID.randomUUID());
  private final Set<AuthorityId> activeAuthorities = new HashSet<>(Set.of(authority));

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final InMemoryFieldReportRepository reports = new InMemoryFieldReportRepository();
  private final InMemoryDistributionPlanRepository plans = new InMemoryDistributionPlanRepository();
  private final InMemoryInventory inventory = new InMemoryInventory(organization);
  private final FakeAiService aiService = new FakeAiService();
  private final List<DomainEvent> events = new ArrayList<>();

  private final GenerateDistributionRecommendationCommandHandler generate =
      new GenerateDistributionRecommendationCommandHandler(
          emergencies,
          zones,
          reports,
          plans,
          aiService,
          inventory,
          inventory,
          activeAuthorities::contains,
          events::add,
          CLOCK);
  private final ModifyDistributionPlanCommandHandler modify =
      new ModifyDistributionPlanCommandHandler(
          plans, inventory, activeAuthorities::contains, events::add, CLOCK);
  private final ApproveDistributionPlanCommandHandler approve =
      new ApproveDistributionPlanCommandHandler(
          plans, activeAuthorities::contains, events::add, CLOCK);
  private final RejectDistributionPlanCommandHandler reject =
      new RejectDistributionPlanCommandHandler(
          plans, inventory, activeAuthorities::contains, events::add, CLOCK);
  private final DistributionPlanQueryService queries =
      new DistributionPlanQueryService(plans, emergencies, zones);
  private final List<List<Object>> brigadeRequests = new ArrayList<>();
  private Optional<BrigadeId> freeBrigade = Optional.of(new BrigadeId(UUID.randomUUID()));
  private final AssignDistributionPlanBrigadeCommandHandler assignBrigade =
      new AssignDistributionPlanBrigadeCommandHandler(
          plans,
          zones,
          (organizationId, planId, zoneId, zoneLocation) -> {
            brigadeRequests.add(List.of(organizationId, planId, zoneId, zoneLocation));
            return freeBrigade;
          },
          activeAuthorities::contains);

  private final Emergency emergency = emergencies.save(emergency());
  private final ResourceId water = inventory.add("Agua 2.5 L", "WATER", 500);
  private final ResourceId food = inventory.add("Kit de alimentos", "FOOD", 300);

  @Test
  void recommendationProposesAPlanForEachZoneThatReceivesResourcesAndReservesIt() {
    Zone altoPeru = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER, Need.FOOD);
    Zone pedregal = prioritizedZone("Quebrada Pedregal", 41.0, Need.WATER);

    GeneratedRecommendation result = generate.handle(command());

    DistributionPlan plan = result.plans().getFirst();
    assertThat(result.plans()).hasSize(1);
    assertThat(plan.zoneId()).isEqualTo(altoPeru.id());
    assertThat(plan.organizationId()).isEqualTo(organization);
    assertThat(plan.coverage()).isEqualTo(Coverage.FULL);
    assertThat(plan.createdAt()).isEqualTo(NOW);
    assertThat(plan.distributedQuantities())
        .containsExactlyInAnyOrder(
            new ResourceQuantity(water, 500), new ResourceQuantity(food, 300));
    assertThat(inventory.reservationOf(plan.id()))
        .containsExactlyInAnyOrderElementsOf(plan.distributedQuantities());
    assertThat(result.unattendedZones())
        .extracting(unattended -> unattended.zone().id(), UnattendedZone::coverage)
        .containsExactly(tuple(pedregal.id(), Coverage.NONE));
    assertThat(result.justification()).isNotBlank();
    assertThat(result.modelVersion()).isEqualTo(FakeAiService.MODEL_VERSION);
    assertThat(inventory.available(water)).isZero();
    assertThat(events).hasAtLeastOneElementOfType(DistributionPlanProposed.class);
  }

  @Test
  void recommendationReceivesTheScorePeopleAndNeedsOfEachZone() {
    Zone zone = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER, Need.FOOD);

    generate.handle(command());

    assertThat(aiService.recommendationCalls())
        .containsExactly(
            List.of(new PrioritizedZone(zone.id(), 78.4, 300, Set.of(Need.WATER, Need.FOOD))));
  }

  @Test
  void zonesWithoutScoreOrWithoutNeedsAreLeftOutAndListed() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    Zone withoutScore = zones.save(registerZone("Castilla"));
    Zone withoutNeeds = prioritizedZone("Quebrada Pedregal", 41.0);

    GeneratedRecommendation result = generate.handle(command());

    assertThat(result.excludedZones())
        .extracting(excluded -> excluded.zone().id(), ExcludedZone::reason)
        .containsExactlyInAnyOrder(
            tuple(withoutScore.id(), ExclusionReason.NO_SCORE),
            tuple(withoutNeeds.id(), ExclusionReason.NO_NEEDS));
    assertThat(aiService.recommendationCalls().getFirst()).hasSize(1);
  }

  @Test
  void partiallyCoveredZoneGetsAPlanAndIsListedAsUnattended() {
    Zone zone = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER, Need.SHELTER);

    GeneratedRecommendation result = generate.handle(command());

    assertThat(result.plans())
        .singleElement()
        .extracting(DistributionPlan::coverage)
        .isEqualTo(Coverage.PARTIAL);
    assertThat(result.unattendedZones())
        .extracting(unattended -> unattended.zone().id())
        .containsExactly(zone.id());
  }

  @Test
  void newRecommendationWaitsUntilThePendingPlansAreDecided() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();

    assertThatThrownBy(() -> generate.handle(command()))
        .isInstanceOf(PendingDistributionPlansException.class);

    reject.handle(
        new RejectDistributionPlanCommand(plan.id(), authority, organization, "Vía bloqueada"));
    assertThat(generate.handle(command()).plans()).hasSize(1);
  }

  @Test
  void zoneWithAnApprovedPlanGetsAnotherOneOnlyForWhatItStillLacks() {
    Zone zone = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER, Need.FOOD);
    DistributionPlan first = generate.handle(command()).plans().getFirst();
    modify.handle(
        new ModifyDistributionPlanCommand(
            first.id(),
            authority,
            organization,
            List.of(new ResourceQuantity(food, 0)),
            "Los víveres llegan en el segundo viaje"));
    approve.handle(new ApproveDistributionPlanCommand(first.id(), authority, organization));

    DistributionPlan second = generate.handle(command()).plans().getFirst();

    assertThat(aiService.recommendationCalls().getLast())
        .singleElement()
        .extracting(PrioritizedZone::alreadyAllocated)
        .isEqualTo(Map.of(Need.WATER, 500));
    assertThat(second.id()).isNotEqualTo(first.id());
    assertThat(second.zoneId()).isEqualTo(zone.id());
    assertThat(second.distributedQuantities()).containsExactly(new ResourceQuantity(food, 300));
    assertThat(inventory.reservationOf(first.id()))
        .containsExactly(new ResourceQuantity(water, 500));
    assertThat(queries.handle(new GetDistributionPlansByZoneIdQuery(zone.id(), organization)))
        .extracting(DistributionPlan::status)
        .containsExactlyInAnyOrder(
            DistributionPlanStatus.PROPOSED, DistributionPlanStatus.APPROVED);
  }

  @Test
  void zoneWhoseNeedsApprovedPlansCoverGetsNothingMore() {
    Zone covered = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan first = generate.handle(command()).plans().getFirst();
    approve.handle(new ApproveDistributionPlanCommand(first.id(), authority, organization));
    Zone other = prioritizedZone("Quebrada Pedregal", 41.0, Need.FOOD);

    GeneratedRecommendation second = generate.handle(command());

    assertThat(second.plans()).extracting(DistributionPlan::zoneId).containsExactly(other.id());
    assertThat(second.unattendedZones()).isEmpty();
    assertThat(queries.handle(new GetDistributionPlansByZoneIdQuery(covered.id(), organization)))
        .containsExactly(plans.findById(first.id()).orElseThrow());
  }

  @Test
  void rejectedPlansGiveTheZoneNothing() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan first = generate.handle(command()).plans().getFirst();
    reject.handle(
        new RejectDistributionPlanCommand(first.id(), authority, organization, "Vía bloqueada"));

    generate.handle(command());

    assertThat(aiService.recommendationCalls().getLast())
        .singleElement()
        .extracting(PrioritizedZone::alreadyAllocated)
        .isEqualTo(Map.of());
  }

  @Test
  void recommendationNeedsAnActiveEmergencyPrioritizedZonesAndStock() {
    assertThatThrownBy(() -> generate.handle(command()))
        .isInstanceOf(NoPrioritizedZonesException.class);

    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    InMemoryInventory empty = new InMemoryInventory(organization);
    var withoutStock =
        new GenerateDistributionRecommendationCommandHandler(
            emergencies,
            zones,
            reports,
            plans,
            aiService,
            empty,
            empty,
            activeAuthorities::contains,
            events::add,
            CLOCK);
    assertThatThrownBy(() -> withoutStock.handle(command()))
        .isInstanceOf(NoAvailableStockException.class);

    emergency.close();
    emergencies.save(emergency);
    assertThatThrownBy(() -> generate.handle(command()))
        .isInstanceOf(EmergencyClosedForDistributionException.class);
  }

  @Test
  void modificationChangesTheReservationAndKeepsTheRecommendation() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER, Need.FOOD);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();

    DistributionPlan modified =
        modify.handle(
            new ModifyDistributionPlanCommand(
                plan.id(),
                authority,
                organization,
                List.of(new ResourceQuantity(water, 200), new ResourceQuantity(food, 0)),
                "La zona recibió víveres de otra ONG"));

    assertThat(modified.distributedQuantities()).containsExactly(new ResourceQuantity(water, 200));
    assertThat(inventory.reservationOf(plan.id()))
        .containsExactly(new ResourceQuantity(water, 200));
    assertThat(inventory.available(water)).isEqualTo(300);
    assertThat(inventory.available(food)).isEqualTo(300);
    assertThat(modified.items())
        .extracting(item -> item.recommendedQuantity().orElseThrow())
        .containsExactlyInAnyOrder(500, 300);
  }

  @Test
  void approvalRecordsTheAuthorityAndKeepsTheReservation() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();

    DistributionPlan approved =
        approve.handle(new ApproveDistributionPlanCommand(plan.id(), authority, organization));

    assertThat(approved.status()).isEqualTo(DistributionPlanStatus.APPROVED);
    assertThat(approved.decision().orElseThrow().decidedBy()).isEqualTo(authority);
    assertThat(approved.decision().orElseThrow().decidedAt()).isEqualTo(NOW);
    assertThat(inventory.reservationOf(plan.id())).isNotEmpty();
    assertThat(events)
        .filteredOn(DistributionPlanApproved.class::isInstance)
        .singleElement()
        .isEqualTo(
            new DistributionPlanApproved(
                plan.id(), emergency.id(), plan.zoneId(), organization, authority));
  }

  @Test
  void approvedPlanGetsABrigadeForItsZone() {
    Zone zone = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = approvedPlan();

    BrigadeId brigade =
        assignBrigade.handle(
            new AssignDistributionPlanBrigadeCommand(plan.id(), organization, Optional.empty()));

    assertThat(brigade).isEqualTo(freeBrigade.orElseThrow());
    assertThat(brigadeRequests)
        .containsExactly(List.of(organization, plan.id(), zone.id(), Optional.empty()));
  }

  @Test
  void onlyAnApprovedPlanGetsABrigade() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();
    var command =
        new AssignDistributionPlanBrigadeCommand(plan.id(), organization, Optional.of(authority));

    assertThatThrownBy(() -> assignBrigade.handle(command))
        .isInstanceOf(DistributionPlanNotApprovedException.class);
    assertThat(brigadeRequests).isEmpty();
  }

  @Test
  void planStaysWithoutBrigadeWhenNoneIsFree() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = approvedPlan();
    freeBrigade = Optional.empty();
    var command =
        new AssignDistributionPlanBrigadeCommand(plan.id(), organization, Optional.of(authority));

    assertThatThrownBy(() -> assignBrigade.handle(command))
        .isInstanceOf(NoAvailableBrigadeException.class);
    activeAuthorities.clear();
    assertThatThrownBy(() -> assignBrigade.handle(command))
        .isInstanceOf(ActionNotAllowedException.class);
    assertThat(brigadeRequests).hasSize(1);
  }

  @Test
  void rejectionKeepsItsReasonInTheHistoryOfTheZoneAndReleasesTheStock() {
    Zone zone = prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();

    reject.handle(
        new RejectDistributionPlanCommand(plan.id(), authority, organization, "Vía bloqueada"));

    assertThat(inventory.available(water)).isEqualTo(500);
    assertThat(inventory.reservationOf(plan.id())).isEmpty();
    assertThat(queries.handle(new GetDistributionPlansByZoneIdQuery(zone.id(), organization)))
        .singleElement()
        .satisfies(
            rejected -> {
              assertThat(rejected.status()).isEqualTo(DistributionPlanStatus.REJECTED);
              assertThat(rejected.rejectedReason()).contains("Vía bloqueada");
            });
    assertThat(events).hasAtLeastOneElementOfType(DistributionPlanRejected.class);
  }

  @Test
  void userWhoIsNoLongerAnAuthorityCannotDecideAndNothingChanges() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();
    activeAuthorities.clear();

    assertThatThrownBy(
            () ->
                approve.handle(
                    new ApproveDistributionPlanCommand(plan.id(), authority, organization)))
        .isInstanceOf(ActionNotAllowedException.class);
    assertThatThrownBy(() -> generate.handle(command()))
        .isInstanceOf(ActionNotAllowedException.class);
    assertThat(plans.findById(plan.id()).orElseThrow().isPending()).isTrue();
  }

  @Test
  void planOfAnotherOrganizationIsNotFound() {
    prioritizedZone("Sector Alto Perú", 78.4, Need.WATER);
    DistributionPlan plan = generate.handle(command()).plans().getFirst();
    var other = new OrganizationId(UUID.randomUUID());

    assertThatThrownBy(() -> queries.handle(new GetDistributionPlanByIdQuery(plan.id(), other)))
        .isInstanceOf(DistributionPlanNotFoundException.class);
    assertThatThrownBy(
            () -> approve.handle(new ApproveDistributionPlanCommand(plan.id(), authority, other)))
        .isInstanceOf(DistributionPlanNotFoundException.class);
    assertThat(
            queries.handle(
                new GetDistributionPlansByEmergencyIdQuery(emergency.id(), other, null)))
        .isEmpty();
    assertThat(
            queries.handle(
                new GetDistributionPlansByEmergencyIdQuery(
                    emergency.id(), organization, DistributionPlanStatus.PROPOSED)))
        .containsExactly(plan);
  }

  private DistributionPlan approvedPlan() {
    DistributionPlan plan = generate.handle(command()).plans().getFirst();
    return approve.handle(new ApproveDistributionPlanCommand(plan.id(), authority, organization));
  }

  private GenerateDistributionRecommendationCommand command() {
    return new GenerateDistributionRecommendationCommand(emergency.id(), authority, organization);
  }

  /** A zone with 300 people affected, its score and a processed report with these needs. */
  private Zone prioritizedZone(String name, double score, Need... needs) {
    Zone zone = registerZone(name);
    zone.updateUrgencyScore(
        new UrgencyScore(score, PriorityLevel.HIGH, List.of(), "fake-v1", NOW));
    zones.save(zone);
    FieldReport report =
        FieldReport.register(emergency.id(), "Reporte de " + name, ReportSource.AUTHORITY, NOW);
    report.assignToZone(zone);
    report.markProcessed(
        new StructuredFieldData(null, null, null, null, null, null, Set.of(needs), List.of()));
    reports.save(report);
    return zone;
  }

  private Zone registerZone(String name) {
    return zones.save(
        emergency.registerZone(
            name, null, new PopulationImpact(600, 75, 300, 40, 15, 6), DamageLevel.SEVERE));
  }

  private static Emergency emergency() {
    return Emergency.register(
        "Huaico Chosica 2026",
        EmergencyType.LANDSLIDE,
        null,
        new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
        LocalDate.of(2026, 3, 14));
  }
}
