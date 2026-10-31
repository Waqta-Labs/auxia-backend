package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AssignFieldReportZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.CalculateUrgencyScoreCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.StructureFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.UpdateZonePopulationImpactCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignFieldReportZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.CalculateUrgencyScoreCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.StructureFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.UpdateZonePopulationImpactCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneRankingQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.FieldReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneQueryService;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneRankingEntry;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportAssignedToZone;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportProcessed;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.events.UrgencyScoreCalculated;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZonePopulationImpactUpdated;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportAlreadyProcessedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InsufficientScoringDataException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Structuring of field reports, urgency score and ranking with a fake AI Service (US-02-06). */
class UrgencyScoreUseCasesTests {

  private static final AuthorityId AUTHORITY = new AuthorityId(UUID.randomUUID());

  private static final String REPORT_TEXT = "Colapso total de viviendas en Alto Perú.";
  private static final StructuredFieldData CRITICAL_REPORT =
      new StructuredFieldData(
          DamageLevel.CRITICAL, null, 300, 90, null, null, Set.of(Need.WATER), List.of());

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final InMemoryFieldReportRepository reports = new InMemoryFieldReportRepository();
  private final List<DomainEvent> events = new ArrayList<>();
  private final FakeAiService aiService = new FakeAiService();

  private final RegisterZoneCommandHandler registerZone =
      new RegisterZoneCommandHandler(emergencies, zones, events::add);
  private final RegisterFieldReportCommandHandler registerReport =
      new RegisterFieldReportCommandHandler(
          emergencies, zones, reports, Clock.systemDefaultZone(), events::add);
  private final AssignFieldReportZoneCommandHandler assignZone =
      new AssignFieldReportZoneCommandHandler(reports, zones, events::add);
  private final StructureFieldReportCommandHandler structure =
      new StructureFieldReportCommandHandler(reports, aiService, events::add);
  private final UpdateZonePopulationImpactCommandHandler updateImpact =
      new UpdateZonePopulationImpactCommandHandler(zones, events::add);
  private final CalculateUrgencyScoreCommandHandler calculateScore =
      new CalculateUrgencyScoreCommandHandler(zones, reports, aiService, events::add);
  private final ZoneQueryService zoneQueries = new ZoneQueryService(emergencies, zones, reports);
  private final FieldReportQueryService reportQueries =
      new FieldReportQueryService(emergencies, zones, reports);

  private final Emergency emergency = emergencies.save(emergency());

  @Test
  void registeringAZonePublishesZoneRegistered() {
    Zone zone = zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE);

    assertThat(events)
        .containsExactly(
            new ZoneRegistered(zone.id(), emergency.id(), "Sector Alto Perú", AUTHORITY));
  }

  @Test
  void registeringAReportPublishesFieldReportRegistered() {
    FieldReport report = report(zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE));

    assertThat(events).endsWith(new FieldReportRegistered(report.id()));
  }

  @Test
  void assigningAPendingReportPublishesTheZone() {
    Zone zone = zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE);
    FieldReport report =
        registerReport.handle(
            new RegisterFieldReportCommand(
                emergency.id(), null, "Sin referencia de zona", ReportSource.CITIZEN));

    assignZone.handle(new AssignFieldReportZoneCommand(report.id(), zone.id()));

    assertThat(events).endsWith(new FieldReportAssignedToZone(report.id(), zone.id()));
  }

  @Test
  void structuringStoresTheIndicatorsAndPublishesTheZoneOfTheReport() {
    Zone zone = zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE);
    FieldReport report = report(zone);
    aiService.willStructure(REPORT_TEXT, CRITICAL_REPORT);

    FieldReport structured = structure.handle(new StructureFieldReportCommand(report.id()));

    assertThat(structured.structuredData()).contains(CRITICAL_REPORT);
    assertThat(reports.findById(report.id()).orElseThrow().isProcessed()).isTrue();
    assertThat(events).endsWith(new FieldReportProcessed(report.id(), zone.id()));
  }

  @Test
  void processedReportIsNotSentToTheAiServiceAgain() {
    FieldReport report = report(zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE));
    structure.handle(new StructureFieldReportCommand(report.id()));

    assertThatThrownBy(() -> structure.handle(new StructureFieldReportCommand(report.id())))
        .isInstanceOf(FieldReportAlreadyProcessedException.class);
    assertThat(aiService.structuringCalls()).hasSize(1);
  }

  @Test
  void unavailableAiServiceLeavesTheReportNotProcessed() {
    FieldReport report = report(zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE));
    aiService.becomeUnavailable();

    assertThatThrownBy(() -> structure.handle(new StructureFieldReportCommand(report.id())))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    assertThat(reports.findById(report.id()).orElseThrow().isProcessed()).isFalse();
  }

  @Test
  void structuringAnUnknownReportIsRejected() {
    assertThatThrownBy(
            () -> structure.handle(new StructureFieldReportCommand(FieldReportId.generate())))
        .isInstanceOf(FieldReportNotFoundException.class);
  }

  @Test
  void scoreUsesTheMostSevereDataOfTheZoneAndItsProcessedReports() {
    Zone zone = zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE);
    aiService.willStructure(REPORT_TEXT, CRITICAL_REPORT);
    structure.handle(new StructureFieldReportCommand(report(zone).id()));
    report(zone); // registered but not processed: it adds nothing

    Zone scored = calculateScore.handle(new CalculateUrgencyScoreCommand(zone.id()));

    assertThat(aiService.scoringCalls())
        .containsExactly(
            new UrgencyIndicators(
                DamageLevel.CRITICAL, 600, 35, 300, 90, 15, 0, Set.of(Need.WATER)));
    // 40 × 1 (CRITICAL) + 60 × 300 / 600
    assertThat(scored.urgencyScore().orElseThrow().value()).isEqualTo(70.0);
    assertThat(events).endsWith(new UrgencyScoreCalculated(zone.id()));
    assertThat(scored.urgencyScore().orElseThrow().level()).isEqualTo(PriorityLevel.HIGH);
    assertThat(scored.status()).isEqualTo(ZoneStatus.PRIORITIZED);
    assertThat(zones.findById(zone.id()).orElseThrow().urgencyScore()).isPresent();
  }

  @Test
  void zoneWithoutPopulationIsNotSentToTheAiService() {
    Zone zone = zone("Quebrada Pedregal", null, 140, DamageLevel.SEVERE);

    assertThatThrownBy(() -> calculateScore.handle(new CalculateUrgencyScoreCommand(zone.id())))
        .isInstanceOf(InsufficientScoringDataException.class);
    assertThat(aiService.scoringCalls()).isEmpty();
  }

  @Test
  void populationAddedLaterKeepsTheOtherCountsAndAllowsTheScore() {
    Zone zone = zone("Quebrada Pedregal", null, 140, DamageLevel.SEVERE);

    Zone updated =
        updateImpact.handle(
            new UpdateZonePopulationImpactCommand(zone.id(), 700, null, 150, null, null, null));

    assertThat(updated.populationImpact()).isEqualTo(new PopulationImpact(700, 35, 150, 40, 15, 0));
    assertThat(events).endsWith(new ZonePopulationImpactUpdated(zone.id()));
    assertThat(calculateScore.handle(new CalculateUrgencyScoreCommand(zone.id())).urgencyScore())
        .isPresent();
  }

  @Test
  void scoreOfAnUnknownZoneIsRejected() {
    assertThatThrownBy(
            () -> calculateScore.handle(new CalculateUrgencyScoreCommand(ZoneId.generate())))
        .isInstanceOf(ZoneNotFoundException.class);
  }

  @Test
  void rankingGoesFromTheMostUrgentZoneAndLeavesZonesWithoutScoreLast() {
    Zone unscored = zone("Sin población", null, 140, DamageLevel.CRITICAL);
    Zone moderate = scoredZone("Moderada", DamageLevel.MODERATE); // 20 + 30 = 50
    Zone critical = scoredZone("Crítica", DamageLevel.CRITICAL); // 40 + 30 = 70
    Zone low = scoredZone("Leve", DamageLevel.LOW); // 10 + 30 = 40

    assertThat(rank(GetZoneRankingQuery.all(emergency.id())))
        .containsExactly(critical.id(), moderate.id(), low.id(), unscored.id());
  }

  @Test
  void scoreRangeKeepsOnlyTheScoredZonesInsideIt() {
    zone("Sin población", null, 140, DamageLevel.CRITICAL);
    Zone moderate = scoredZone("Moderada", DamageLevel.MODERATE);
    scoredZone("Crítica", DamageLevel.CRITICAL);
    scoredZone("Leve", DamageLevel.LOW);

    assertThat(rank(new GetZoneRankingQuery(emergency.id(), 45.0, 60.0, null)))
        .containsExactly(moderate.id());
  }

  @Test
  void needFilterKeepsTheZonesWhoseProcessedReportsMentionIt() {
    Zone withWater = zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE);
    zone("Quebrada Pedregal", 600, 140, DamageLevel.SEVERE);
    aiService.willStructure(REPORT_TEXT, CRITICAL_REPORT);
    structure.handle(new StructureFieldReportCommand(report(withWater).id()));

    List<ZoneRankingEntry> ranking =
        zoneQueries.handle(new GetZoneRankingQuery(emergency.id(), null, null, Need.WATER));

    assertThat(ranking).extracting(entry -> entry.zone().id()).containsExactly(withWater.id());
    assertThat(ranking.getFirst().needs()).containsExactly(Need.WATER);
  }

  @Test
  void reportIsFoundById() {
    FieldReport report = report(zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE));

    assertThat(reportQueries.handle(new GetFieldReportByIdQuery(report.id()))).isEqualTo(report);
    assertThatThrownBy(
            () -> reportQueries.handle(new GetFieldReportByIdQuery(FieldReportId.generate())))
        .isInstanceOf(FieldReportNotFoundException.class);
  }

  @Test
  void needsOfARankedZoneJoinItsReportsInTheOrderOfTheEnumeration() {
    Zone zone = zone("Sector Alto Perú", 600, 140, DamageLevel.SEVERE);
    structureReport(zone, "Faltan carpas y medicinas", Set.of(Need.SHELTER, Need.MEDICINE));
    structureReport(zone, "Urge agua potable y carpas", Set.of(Need.WATER, Need.SHELTER));

    assertThat(zoneQueries.handle(GetZoneRankingQuery.all(emergency.id())).getFirst().needs())
        .containsExactly(Need.WATER, Need.SHELTER, Need.MEDICINE);
  }

  private void structureReport(Zone zone, String text, Set<Need> needs) {
    aiService.willStructure(
        text, new StructuredFieldData(null, null, null, null, null, null, needs, List.of()));
    FieldReport report =
        registerReport.handle(
            RegisterFieldReportCommand.forZone(zone.id(), text, ReportSource.AUTHORITY));
    structure.handle(new StructureFieldReportCommand(report.id()));
  }

  private List<ZoneId> rank(GetZoneRankingQuery query) {
    return zoneQueries.handle(query).stream().map(entry -> entry.zone().id()).toList();
  }

  private Zone scoredZone(String name, DamageLevel damageLevel) {
    Zone zone = zone(name, 600, 300, damageLevel);
    return calculateScore.handle(new CalculateUrgencyScoreCommand(zone.id()));
  }

  private Zone zone(String name, Integer population, int peopleAffected, DamageLevel damage) {
    return registerZone.handle(
        new RegisterZoneCommand(
            emergency.id(),
            name,
            null,
            new PopulationImpact(population, 35, peopleAffected, 40, 15, 0),
            damage,
            AUTHORITY));
  }

  private FieldReport report(Zone zone) {
    return registerReport.handle(
        RegisterFieldReportCommand.forZone(zone.id(), REPORT_TEXT, ReportSource.AUTHORITY));
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
