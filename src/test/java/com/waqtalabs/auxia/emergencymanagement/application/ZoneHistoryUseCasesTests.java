package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.eventhandlers.ZoneDecisionLogEventHandler;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorityDirectory;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneHistoryQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneHistoryEntry;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneHistoryQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanModified;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanRejected;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportCorrected;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ShiftHandedOver;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneManualPriorityRemoved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZonePrioritizedManually;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ShiftHandoverId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** History of decisions of a zone (US-20), fed by the events of each decision. */
class ZoneHistoryUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-15T08:00:00Z"), ZoneOffset.UTC);
  private static final OrganizationId COER = new OrganizationId(UUID.randomUUID());
  private static final AuthorityId DAY_SHIFT = new AuthorityId(UUID.randomUUID());
  private static final AuthorityId NIGHT_SHIFT = new AuthorityId(UUID.randomUUID());
  private static final ResourceId WATER = new ResourceId(UUID.randomUUID());

  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final InMemoryDistributionPlanRepository plans = new InMemoryDistributionPlanRepository();
  private final InMemoryZoneDecisionRepository decisions = new InMemoryZoneDecisionRepository();
  private final ZoneDecisionLogEventHandler log =
      new ZoneDecisionLogEventHandler(decisions, plans, CLOCK);
  private final ZoneHistoryQueryService history =
      new ZoneHistoryQueryService(
          zones,
          decisions,
          new AuthorityDirectory() {
            @Override
            public Optional<OrganizationId> organizationOf(AuthorityId authority) {
              return Optional.of(COER);
            }

            @Override
            public Map<AuthorityId, String> fullNames(Collection<AuthorityId> authorities) {
              return Map.of(DAY_SHIFT, "Rosa Quispe", NIGHT_SHIFT, "Luis Mamani");
            }
          });

  private final Emergency emergency =
      Emergency.register(
          "Huaico Chosica 2026",
          EmergencyType.LANDSLIDE,
          null,
          new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
          LocalDate.of(2026, 3, 14));
  private final Zone zone =
      zones.save(
          emergency.registerZone(
              "Sector Alto Perú",
              null,
              new PopulationImpact(600, 35, 140, 40, 15, 0),
              DamageLevel.SEVERE));

  @Test
  void everyDecisionIsListedInChronologicalOrderWithItsAuthority() {
    log.on(new ZoneRegistered(zone.id(), emergency.id(), zone.name(), DAY_SHIFT));
    DistributionPlan rejected = plan();
    rejected.modifyItems(
        List.of(new ResourceQuantity(WATER, 80)), DAY_SHIFT, "Solo hay 80", at(9));
    plans.save(rejected);
    log.on(new DistributionPlanModified(rejected.id(), DAY_SHIFT));
    rejected.reject(DAY_SHIFT, "La carretera está cortada", at(10));
    plans.save(rejected);
    log.on(new DistributionPlanRejected(rejected.id(), zone.id(), DAY_SHIFT));
    log.on(new ZonePrioritizedManually(zone.id(), DAY_SHIFT, "Riesgo inminente", at(11)));
    UUID report = UUID.randomUUID();
    log.on(
        new FieldReportCorrected(
            new FieldReportId(report), zone.id(), DAY_SHIFT, "Conteo en campo", at(12)));
    var handover = ShiftHandoverId.generate();
    log.on(
        new ShiftHandedOver(
            handover, DAY_SHIFT, NIGHT_SHIFT, List.of(zone.id()), "Sin acceso", at(20)));
    log.on(new ZoneManualPriorityRemoved(zone.id(), NIGHT_SHIFT, at(22)));
    DistributionPlan approved = plan();
    approved.approve(NIGHT_SHIFT, at(23));
    plans.save(approved);
    log.on(
        new DistributionPlanApproved(
            approved.id(), emergency.id(), zone.id(), COER, NIGHT_SHIFT));

    List<ZoneHistoryEntry> entries = history.handle(new GetZoneHistoryQuery(zone.id()));

    assertThat(entries)
        .extracting(entry -> entry.decision().type())
        .containsExactly(
            ZoneDecisionType.ZONE_REGISTERED,
            ZoneDecisionType.PLAN_MODIFIED,
            ZoneDecisionType.PLAN_REJECTED,
            ZoneDecisionType.MANUAL_PRIORITY_SET,
            ZoneDecisionType.REPORT_CORRECTED,
            ZoneDecisionType.SHIFT_HANDED_OVER,
            ZoneDecisionType.MANUAL_PRIORITY_REMOVED,
            ZoneDecisionType.PLAN_APPROVED);
    assertThat(entries)
        .extracting(entry -> entry.decision().detail().orElse(null))
        .containsExactly(
            null,
            "Solo hay 80",
            "La carretera está cortada",
            "Riesgo inminente",
            "Conteo en campo",
            "Sin acceso",
            null,
            null);
    assertThat(entries)
        .extracting(ZoneHistoryEntry::decidedByName)
        .containsExactly(
            "Rosa Quispe",
            "Rosa Quispe",
            "Rosa Quispe",
            "Rosa Quispe",
            "Rosa Quispe",
            "Rosa Quispe",
            "Luis Mamani",
            "Luis Mamani");
    assertThat(entries.get(0).decision().decidedAt()).isEqualTo(at(8));
    assertThat(entries.get(2).decision().subjectId()).contains(rejected.id().value());
    assertThat(entries.get(4).decision().subjectId()).contains(report);
    ZoneHistoryEntry shift = entries.get(5);
    assertThat(shift.decision().handedOverTo()).contains(NIGHT_SHIFT);
    assertThat(shift.handedOverToName()).isEqualTo("Luis Mamani");
    assertThat(shift.decision().subjectId()).contains(handover.value());
  }

  @Test
  void aCorrectionOfAReportWithoutZoneIsNotInAnyHistory() {
    log.on(
        new FieldReportCorrected(
            new FieldReportId(UUID.randomUUID()), null, DAY_SHIFT, "Conteo", at(9)));

    assertThat(history.handle(new GetZoneHistoryQuery(zone.id()))).isEmpty();
  }

  @Test
  void aZoneThatDoesNotExistHasNoHistory() {
    assertThatThrownBy(() -> history.handle(new GetZoneHistoryQuery(ZoneId.generate())))
        .isInstanceOf(ZoneNotFoundException.class);
  }

  private DistributionPlan plan() {
    return plans.save(
        DistributionPlan.propose(
            emergency.id(),
            zone.id(),
            COER,
            List.of(new ResourceQuantity(WATER, 120)),
            Coverage.FULL,
            "Cubre el agua de la zona",
            "rules-v1",
            at(8)));
  }

  private static LocalDateTime at(int hour) {
    return LocalDateTime.of(2026, 3, 15, hour, 0);
  }
}
