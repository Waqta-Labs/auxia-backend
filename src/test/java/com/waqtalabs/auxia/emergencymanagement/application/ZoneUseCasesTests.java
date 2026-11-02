package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneRankingQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneQueryService;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneRankingEntry;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZoneUseCasesTests {

  private static final AuthorityId AUTHORITY = new AuthorityId(UUID.randomUUID());

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final List<DomainEvent> events = new ArrayList<>();
  private final RegisterZoneCommandHandler register =
      new RegisterZoneCommandHandler(emergencies, zones, events::add);
  private final InMemoryFieldReportRepository reports = new InMemoryFieldReportRepository();
  private final ZoneQueryService queries = new ZoneQueryService(emergencies, zones, reports);

  private final Emergency emergency =
      emergencies.save(
          Emergency.register(
              "Huaico Chosica 2026",
              EmergencyType.LANDSLIDE,
              null,
              new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
              LocalDate.of(2026, 3, 14)));

  @Test
  void zoneOfAnActiveEmergencyIsStored() {
    Zone zone = register.handle(command(emergency.id(), "Sector Alto Perú"));

    assertThat(zones.findById(zone.id())).contains(zone);
    assertThat(zone.emergencyId()).isEqualTo(emergency.id());
    assertThat(zone.status()).isEqualTo(ZoneStatus.REGISTERED);
  }

  @Test
  void zoneOfAnUnknownEmergencyIsRejected() {
    assertThatThrownBy(() -> register.handle(command(EmergencyId.generate(), "Castilla")))
        .isInstanceOf(EmergencyNotFoundException.class);
  }

  @Test
  void zoneOfAClosedEmergencyIsRejectedAndNotStored() {
    emergency.close();

    assertThatThrownBy(() -> register.handle(command(emergency.id(), "Sector Alto Perú")))
        .isInstanceOf(EmergencyClosedException.class);
    assertThat(zones.findByEmergencyId(emergency.id())).isEmpty();
  }

  @Test
  void queriesFindAZoneAndTheZonesOfAnEmergency() {
    Zone first = register.handle(command(emergency.id(), "Sector Alto Perú"));
    Zone second = register.handle(command(emergency.id(), "Quebrada Pedregal"));

    assertThat(queries.handle(new GetZoneByIdQuery(first.id()))).isEqualTo(first);
    assertThat(queries.handle(GetZoneRankingQuery.all(emergency.id())))
        .extracting(ZoneRankingEntry::zone)
        .containsExactly(first, second);
  }

  @Test
  void queriesRejectUnknownZonesAndEmergencies() {
    assertThatThrownBy(() -> queries.handle(new GetZoneByIdQuery(ZoneId.generate())))
        .isInstanceOf(ZoneNotFoundException.class);
    assertThatThrownBy(
            () -> queries.handle(GetZoneRankingQuery.all(EmergencyId.generate())))
        .isInstanceOf(EmergencyNotFoundException.class);
  }

  private static RegisterZoneCommand command(EmergencyId emergencyId, String name) {
    return new RegisterZoneCommand(
        emergencyId,
        name,
        null,
        new PopulationImpact(600, 35, 140, 40, 15, 0),
        DamageLevel.SEVERE,
        AUTHORITY);
  }
}
