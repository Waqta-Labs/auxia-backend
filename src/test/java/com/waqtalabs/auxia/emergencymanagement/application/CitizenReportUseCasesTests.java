package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterCitizenReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterCitizenReportCommandHandler.Registration;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.StructureFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.StructureFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetCitizenReportProgressQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportSosQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress.Stage;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.CitizenReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportWithoutSosException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CitizenReportUseCasesTests {

  /** 2026-11-06 09:41 in Lima, 14:41 in UTC. */
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-06T14:41:05Z"), ZoneOffset.ofHours(-5));

  private static final Geolocation ALTO_PERU = new Geolocation(-11.9436, -76.7078);

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final InMemoryFieldReportRepository fieldReports = new InMemoryFieldReportRepository();
  private final InMemoryCitizenReportRepository citizenReports =
      new InMemoryCitizenReportRepository();
  private final InMemoryDistributionPlanRepository plans = new InMemoryDistributionPlanRepository();
  private final List<DomainEvent> events = new ArrayList<>();
  private final List<String> acquiredBy = new ArrayList<>();
  private final Map<DistributionPlanId, Geolocation> brigadeBases = new HashMap<>();
  private boolean limitReached;

  private final RegisterCitizenReportCommandHandler register =
      new RegisterCitizenReportCommandHandler(
          citizenReports,
          emergencies,
          zones,
          fieldReports,
          sender -> {
            if (limitReached) {
              throw new RateLimitExceededException(Duration.ofMinutes(3));
            }
            acquiredBy.add(sender);
          },
          CLOCK,
          events::add);
  private final CitizenReportQueryService queries =
      new CitizenReportQueryService(
          citizenReports,
          fieldReports,
          zones,
          plans,
          plan -> Optional.ofNullable(brigadeBases.get(plan)));

  /** The NLP found one child in the text, and nothing about the other people nor the needs. */
  private final StructureFieldReportCommandHandler structure =
      new StructureFieldReportCommandHandler(
          fieldReports,
          citizenReports,
          text ->
              new StructuredFieldData(
                  DamageLevel.SEVERE,
                  null,
                  null,
                  1,
                  null,
                  null,
                  Set.of(),
                  List.of("familiesAffected", "peopleAffected", "elderlyPeople", "needs")),
          events::add);

  private final Emergency chosica = emergency("Huaico Chosica 2026");
  private final Zone altoPeru = zone(chosica, "Sector Alto Perú", ALTO_PERU);

  @Test
  void sosNearAZoneBecomesAFieldReportOfTheZonePendingReview() {
    Registration registration = register.handle(sos(UUID.randomUUID(), nearAltoPeru()));

    assertThat(registration.created()).isTrue();
    CitizenReport sos = registration.report();
    FieldReport report = fieldReports.findById(sos.fieldReportId().orElseThrow()).orElseThrow();
    assertThat(report.emergencyId()).isEqualTo(chosica.id());
    assertThat(report.zoneId()).contains(altoPeru.id());
    assertThat(report.source()).isEqualTo(ReportSource.CITIZEN);
    assertThat(report.rawText()).isEqualTo("SOS Médico. Necesitan ayuda 3 niños y 2 heridos.");
    assertThat(report.isProcessed()).isFalse();
    assertThat(events).containsExactly(new FieldReportRegistered(report.id()));
  }

  @Test
  void sosKeepsWhatTheCitizenSentAndTheTimeOfThePhoneInTheTimeZoneOfTheServer() {
    var command =
        new RegisterCitizenReportCommand(
            new CitizenReportId(UUID.randomUUID()),
            SosCategory.RESCUE,
            "  Vía bloqueada, hay heridos.  ",
            Instant.parse("2026-11-06T13:00:00Z"),
            Optional.of(nearAltoPeru()),
            new VulnerablePeople(1, 2, 3),
            Set.of(AccessCondition.NO_CELL_SIGNAL, AccessCondition.ROAD_BLOCKED),
            Optional.empty(),
            "190.12.4.7");

    CitizenReport sos = register.handle(command).report();

    assertThat(citizenReports.findById(sos.id())).contains(sos);
    assertThat(sos.category()).isEqualTo(SosCategory.RESCUE);
    assertThat(sos.text()).isEqualTo("Vía bloqueada, hay heridos.");
    assertThat(sos.reportedAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 8, 0));
    assertThat(sos.receivedAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 9, 41, 5));
    assertThat(sos.location()).contains(nearAltoPeru());
    assertThat(sos.people()).isEqualTo(new VulnerablePeople(1, 2, 3));
    assertThat(sos.accessConditions())
        .containsExactly(AccessCondition.ROAD_BLOCKED, AccessCondition.NO_CELL_SIGNAL);
    FieldReport report = fieldReports.findById(sos.fieldReportId().orElseThrow()).orElseThrow();
    assertThat(report.reportedAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 8, 0));
  }

  @Test
  void phoneClockAheadOfTheServerDoesNotDateTheReportInTheFuture() {
    var command = sos(UUID.randomUUID(), nearAltoPeru(), Instant.parse("2026-11-06T18:00:00Z"));

    CitizenReport sos = register.handle(command).report();

    FieldReport report = fieldReports.findById(sos.fieldReportId().orElseThrow()).orElseThrow();
    assertThat(report.reportedAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 9, 41, 5));
  }

  @Test
  void sameSosSentAgainIsReturnedWithoutStoringItTwiceNorCountingIt() {
    UUID id = UUID.randomUUID();
    CitizenReport first = register.handle(sos(id, nearAltoPeru())).report();
    limitReached = true;

    Registration retry = register.handle(sos(id, nearAltoPeru()));

    assertThat(retry.created()).isFalse();
    assertThat(retry.report()).isEqualTo(first);
    assertThat(citizenReports.count()).isEqualTo(1);
    assertThat(fieldReports.findByEmergencyId(chosica.id())).hasSize(1);
    assertThat(acquiredBy).containsExactly("190.12.4.7");
  }

  @Test
  void newSosOfASenderOverTheLimitIsRejectedAndNothingIsStored() {
    limitReached = true;
    var command = sos(UUID.randomUUID(), nearAltoPeru());

    assertThatThrownBy(() -> register.handle(command))
        .isInstanceOf(RateLimitExceededException.class);
    assertThat(citizenReports.count()).isZero();
    assertThat(fieldReports.findByEmergencyId(chosica.id())).isEmpty();
  }

  @Test
  void sosFarFromTheZonesGoesToTheEmergencyOfTheNearestOnePendingZoneAssignment() {
    emergency("Inundación Piura 2026");
    Geolocation twentyKilometersAway = new Geolocation(-11.7636, -76.7078);

    CitizenReport sos = register.handle(sos(UUID.randomUUID(), twentyKilometersAway)).report();

    FieldReport report = fieldReports.findById(sos.fieldReportId().orElseThrow()).orElseThrow();
    assertThat(report.emergencyId()).isEqualTo(chosica.id());
    assertThat(report.isPendingZoneAssignment()).isTrue();
  }

  @Test
  void sosWithoutLocationGoesToTheOnlyActiveEmergencyAndToTheZoneItMentions() {
    var command =
        new RegisterCitizenReportCommand(
            new CitizenReportId(UUID.randomUUID()),
            SosCategory.SUPPLIES,
            "Falta agua en el Sector Alto Perú",
            CLOCK.instant(),
            Optional.empty(),
            VulnerablePeople.NONE,
            Set.of(),
            Optional.empty(),
            "190.12.4.7");

    CitizenReport sos = register.handle(command).report();

    FieldReport report = fieldReports.findById(sos.fieldReportId().orElseThrow()).orElseThrow();
    assertThat(report.emergencyId()).isEqualTo(chosica.id());
    assertThat(report.zoneId()).contains(altoPeru.id());
  }

  @Test
  void sosThatCannotBePlacedIsKeptWithoutAFieldReport() {
    emergency("Inundación Piura 2026");
    Geolocation cusco = new Geolocation(-13.5320, -71.9675);

    CitizenReport sos = register.handle(sos(UUID.randomUUID(), cusco)).report();

    assertThat(citizenReports.findById(sos.id())).contains(sos);
    assertThat(sos.fieldReportId()).isEmpty();
    assertThat(events).isEmpty();
    assertThat(progress(sos)).isEqualTo(stage(Stage.RECEIVED));
  }

  @Test
  void detailOfAnEarlierSosGoesToTheSameZoneEvenWithoutLocation() {
    CitizenReport earlier = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();
    emergency("Inundación Piura 2026");
    var detail =
        new RegisterCitizenReportCommand(
            new CitizenReportId(UUID.randomUUID()),
            SosCategory.MEDICAL,
            "Ahora son 4 heridos.",
            CLOCK.instant(),
            Optional.empty(),
            new VulnerablePeople(3, 0, 4),
            Set.of(),
            Optional.of(earlier.id()),
            "190.12.4.7");

    CitizenReport sos = register.handle(detail).report();

    assertThat(sos.relatedReportId()).contains(earlier.id());
    FieldReport report = fieldReports.findById(sos.fieldReportId().orElseThrow()).orElseThrow();
    assertThat(report.id()).isNotEqualTo(earlier.fieldReportId().orElseThrow());
    assertThat(report.zoneId()).contains(altoPeru.id());
  }

  @Test
  void sosOfAZoneWithoutScoreIsReceived() {
    CitizenReport sos = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();

    assertThat(progress(sos)).isEqualTo(stage(Stage.RECEIVED));
  }

  @Test
  void sosOfAZoneWithScoreIsPrioritized() {
    CitizenReport sos = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();
    scoreAltoPeru(94.0);

    assertThat(progress(sos))
        .isEqualTo(
            new CitizenReportProgress(Stage.PRIORITIZED, Optional.of(94.0), Optional.empty()));
  }

  @Test
  void sosOfAZonePrioritizedManuallyIsPrioritized() {
    CitizenReport sos = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();
    altoPeru.prioritizeManually(
        new AuthorityId(UUID.randomUUID()), "Riesgo de otro huaico", LocalDateTime.now(CLOCK));
    zones.save(altoPeru);

    assertThat(progress(sos)).isEqualTo(stage(Stage.PRIORITIZED));
  }

  @Test
  void sosOfAZoneWhoseApprovedPlanABrigadeTookHasTheBrigadeOnTheWayWithItsEta() {
    CitizenReport sos = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();
    scoreAltoPeru(94.0);
    DistributionPlan plan = approvedPlan();

    assertThat(progress(sos).stage()).isEqualTo(Stage.PRIORITIZED);

    Geolocation sixKilometersAway = new Geolocation(-11.9436, -76.6528);
    brigadeBases.put(plan.id(), sixKilometersAway);

    assertThat(progress(sos))
        .isEqualTo(
            new CitizenReportProgress(
                Stage.BRIGADE_ON_THE_WAY, Optional.of(94.0), Optional.of(12)));
  }

  @Test
  void structuredSosIsCompletedWithWhatTheCitizenMarked() {
    var command =
        new RegisterCitizenReportCommand(
            new CitizenReportId(UUID.randomUUID()),
            SosCategory.RESCUE,
            "Ayuda, se cayó la casa.",
            CLOCK.instant(),
            Optional.of(nearAltoPeru()),
            new VulnerablePeople(3, 0, 2),
            Set.of(AccessCondition.ROAD_BLOCKED, AccessCondition.NO_DRINKING_WATER),
            Optional.empty(),
            "190.12.4.7");
    CitizenReport sos = register.handle(command).report();

    FieldReport report =
        structure.handle(new StructureFieldReportCommand(sos.fieldReportId().orElseThrow()));

    StructuredFieldData data = report.structuredData().orElseThrow();
    assertThat(data.children()).isEqualTo(3);
    assertThat(data.peopleAffected()).isEqualTo(5);
    assertThat(data.elderlyPeople()).isNull();
    assertThat(data.needs()).containsExactly(Need.WATER, Need.MEDICINE);
    assertThat(data.missingVariables()).containsExactly("familiesAffected", "elderlyPeople");
    assertThat(data.damageLevel()).isEqualTo(DamageLevel.SEVERE);
  }

  @Test
  void numberInTheTextAboveTheMarkedOneIsKeptAndAMedicalSosNeedsMedicine() {
    CitizenReport sos = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();
    StructuredFieldData extracted =
        new StructuredFieldData(
            DamageLevel.CRITICAL, 40, 120, 8, null, null, Set.of(Need.FOOD), List.of());

    StructuredFieldData data = sos.completeIndicators(extracted);

    assertThat(data.children()).isEqualTo(8);
    assertThat(data.peopleAffected()).isEqualTo(120);
    assertThat(data.needs()).containsExactly(Need.FOOD, Need.MEDICINE);
    assertThat(data.familiesAffected()).isEqualTo(40);
  }

  @Test
  void fieldReportShowsWhatTheCitizenMarkedInTheSos() {
    CitizenReport sos = register.handle(sos(UUID.randomUUID(), nearAltoPeru())).report();
    FieldReport byAuthority =
        fieldReports.save(
            FieldReport.register(
                chosica.id(), "Informe del COER", ReportSource.AUTHORITY, LocalDateTime.now(CLOCK)));

    assertThat(queries.handle(new GetFieldReportSosQuery(sos.fieldReportId().orElseThrow())))
        .isEqualTo(sos);
    assertThatThrownBy(() -> queries.handle(new GetFieldReportSosQuery(byAuthority.id())))
        .isInstanceOf(FieldReportWithoutSosException.class);
  }

  @Test
  void unknownSosIsNotFound() {
    var query = new GetCitizenReportProgressQuery(new CitizenReportId(UUID.randomUUID()));

    assertThatThrownBy(() -> queries.handle(query))
        .isInstanceOf(CitizenReportNotFoundException.class);
  }

  private CitizenReportProgress progress(CitizenReport sos) {
    return queries.handle(new GetCitizenReportProgressQuery(sos.id()));
  }

  private static CitizenReportProgress stage(Stage stage) {
    return new CitizenReportProgress(stage, Optional.empty(), Optional.empty());
  }

  /** About 1 km from the zone. */
  private static Geolocation nearAltoPeru() {
    return new Geolocation(-11.9526, -76.7078);
  }

  private static RegisterCitizenReportCommand sos(UUID id, Geolocation location) {
    return sos(id, location, CLOCK.instant());
  }

  private static RegisterCitizenReportCommand sos(
      UUID id, Geolocation location, Instant reportedAt) {
    return new RegisterCitizenReportCommand(
        new CitizenReportId(id),
        SosCategory.MEDICAL,
        "SOS Médico. Necesitan ayuda 3 niños y 2 heridos.",
        reportedAt,
        Optional.of(location),
        new VulnerablePeople(3, 0, 2),
        Set.of(AccessCondition.ROAD_BLOCKED),
        Optional.empty(),
        "190.12.4.7");
  }

  private void scoreAltoPeru(double value) {
    altoPeru.updateUrgencyScore(
        new UrgencyScore(
            value, PriorityLevel.CRITICAL, List.of(), "fake-v1", LocalDateTime.now(CLOCK)));
    zones.save(altoPeru);
  }

  private DistributionPlan approvedPlan() {
    DistributionPlan plan =
        DistributionPlan.propose(
            chosica.id(),
            altoPeru.id(),
            new OrganizationId(UUID.randomUUID()),
            List.of(new ResourceQuantity(new ResourceId(UUID.randomUUID()), 100)),
            Coverage.FULL,
            "Proporcional al score",
            "rules-v1",
            LocalDateTime.now(CLOCK));
    plan.approve(new AuthorityId(UUID.randomUUID()), LocalDateTime.now(CLOCK));
    return plans.save(plan);
  }

  private Emergency emergency(String name) {
    return emergencies.save(
        Emergency.register(
            name,
            EmergencyType.LANDSLIDE,
            null,
            new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
            LocalDate.of(2026, 3, 14)));
  }

  private Zone zone(Emergency emergency, String name, Geolocation location) {
    return zones.save(
        emergency.registerZone(
            name, location, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.SEVERE));
  }
}
