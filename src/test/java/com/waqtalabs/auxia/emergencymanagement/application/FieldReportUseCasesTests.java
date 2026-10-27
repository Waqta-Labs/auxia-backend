package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AssignFieldReportZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignFieldReportZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportsByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportsByZoneIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.FieldReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportAlreadyAssignedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneOutsideEmergencyException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FieldReportUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-14T14:30:00Z"), ZoneOffset.ofHours(-5));

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final InMemoryFieldReportRepository reports = new InMemoryFieldReportRepository();
  private final List<DomainEvent> events = new ArrayList<>();
  private final RegisterFieldReportCommandHandler register =
      new RegisterFieldReportCommandHandler(emergencies, zones, reports, CLOCK, events::add);
  private final AssignFieldReportZoneCommandHandler assign =
      new AssignFieldReportZoneCommandHandler(reports, zones, events::add);
  private final FieldReportQueryService queries =
      new FieldReportQueryService(emergencies, zones, reports);

  private final Emergency chosica = emergency("Huaico Chosica 2026");
  private final Zone altoPeru = zone(chosica, "Alto Perú");
  private final Zone pedregal = zone(chosica, "Quebrada Pedregal");

  @Test
  void reportOfAZoneBelongsToItsEmergencyAndIsStoredWithTheCurrentTime() {
    FieldReport report =
        register.handle(
            RegisterFieldReportCommand.forZone(
                altoPeru.id(), "Hay 35 familias afectadas", ReportSource.AUTHORITY));

    assertThat(reports.findById(report.id())).contains(report);
    assertThat(report.emergencyId()).isEqualTo(chosica.id());
    assertThat(report.zoneId()).contains(altoPeru.id());
    assertThat(report.reportedAt()).isEqualTo(LocalDateTime.of(2026, 3, 14, 9, 30));
  }

  @Test
  void reportOfAnEmergencyUsesTheZoneIndicatedByTheAuthority() {
    FieldReport report =
        register.handle(
            new RegisterFieldReportCommand(
                chosica.id(), pedregal.id(), "Derrumbe en Alto Perú", ReportSource.AUTHORITY));

    assertThat(report.zoneId()).contains(pedregal.id());
  }

  @Test
  void reportOfAnEmergencyIdentifiesTheZoneMentionedInTheText() {
    FieldReport report = registerForEmergency(chosica.id(), "Derrumbe de viviendas en Alto Perú.");

    assertThat(report.zoneId()).contains(altoPeru.id());
  }

  @Test
  void reportWithoutAnIdentifiableZoneStaysPending() {
    FieldReport report = registerForEmergency(chosica.id(), "Hay 35 familias afectadas.");

    assertThat(report.isPendingZoneAssignment()).isTrue();
    assertThat(queries.handle(new GetFieldReportsByEmergencyIdQuery(chosica.id(), true)))
        .containsExactly(report);
  }

  @Test
  void zoneOfAnotherEmergencyIsRejected() {
    Zone castilla = zone(emergency("Inundación Piura 2026"), "Castilla");
    var command =
        new RegisterFieldReportCommand(
            chosica.id(), castilla.id(), "Calles inundadas", ReportSource.CITIZEN);

    assertThatThrownBy(() -> register.handle(command))
        .isInstanceOf(ZoneOutsideEmergencyException.class);
    assertThat(queries.handle(new GetFieldReportsByEmergencyIdQuery(chosica.id(), false)))
        .isEmpty();
  }

  @Test
  void unknownZoneOrEmergencyIsRejected() {
    assertThatThrownBy(
            () ->
                register.handle(
                    RegisterFieldReportCommand.forZone(
                        ZoneId.generate(), "Derrumbe", ReportSource.CITIZEN)))
        .isInstanceOf(ZoneNotFoundException.class);
    assertThatThrownBy(() -> registerForEmergency(EmergencyId.generate(), "Derrumbe"))
        .isInstanceOf(EmergencyNotFoundException.class);
  }

  @Test
  void authorityAssignsTheZoneOfAPendingReportOnlyOnce() {
    FieldReport report = registerForEmergency(chosica.id(), "Hay 35 familias afectadas.");

    assign.handle(new AssignFieldReportZoneCommand(report.id(), pedregal.id()));

    assertThat(queries.handle(new GetFieldReportsByZoneIdQuery(pedregal.id())))
        .containsExactly(report);
    assertThat(queries.handle(new GetFieldReportsByEmergencyIdQuery(chosica.id(), true)))
        .isEmpty();
    assertThatThrownBy(
            () -> assign.handle(new AssignFieldReportZoneCommand(report.id(), altoPeru.id())))
        .isInstanceOf(FieldReportAlreadyAssignedException.class);
  }

  @Test
  void assigningAnUnknownReportOrZoneIsRejected() {
    FieldReport report = registerForEmergency(chosica.id(), "Hay 35 familias afectadas.");

    assertThatThrownBy(
            () ->
                assign.handle(
                    new AssignFieldReportZoneCommand(FieldReportId.generate(), altoPeru.id())))
        .isInstanceOf(FieldReportNotFoundException.class);
    assertThatThrownBy(
            () -> assign.handle(new AssignFieldReportZoneCommand(report.id(), ZoneId.generate())))
        .isInstanceOf(ZoneNotFoundException.class);
  }

  @Test
  void queriesRejectUnknownZonesAndEmergencies() {
    assertThatThrownBy(() -> queries.handle(new GetFieldReportsByZoneIdQuery(ZoneId.generate())))
        .isInstanceOf(ZoneNotFoundException.class);
    assertThatThrownBy(
            () ->
                queries.handle(
                    new GetFieldReportsByEmergencyIdQuery(EmergencyId.generate(), false)))
        .isInstanceOf(EmergencyNotFoundException.class);
  }

  @Test
  void commandNeedsAnEmergencyOrAZone() {
    assertThatThrownBy(
            () -> new RegisterFieldReportCommand(null, null, "Derrumbe", ReportSource.CITIZEN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private FieldReport registerForEmergency(EmergencyId emergencyId, String text) {
    return register.handle(
        new RegisterFieldReportCommand(emergencyId, null, text, ReportSource.VOLUNTEER));
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

  private Zone zone(Emergency emergency, String name) {
    return zones.save(
        emergency.registerZone(
            name, null, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.SEVERE));
  }
}
