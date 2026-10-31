package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.HandOverShiftCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterZoneCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.HandOverShiftCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorityDirectory;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZonesInChargeQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ShiftHandedOver;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IneligibleIncomingAuthorityException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ShiftHandoverToSelfException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ShiftHandover;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ShiftHandoverRepository;
import com.waqtalabs.auxia.shared.application.exceptions.ActionNotAllowedException;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Shift change between authorities (US-23), with the zones each one follows. */
class ShiftHandoverUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-15T20:00:00Z"), ZoneOffset.UTC);
  private static final OrganizationId COER = new OrganizationId(UUID.randomUUID());
  private static final AuthorityId OUTGOING = new AuthorityId(UUID.randomUUID());
  private static final AuthorityId INCOMING = new AuthorityId(UUID.randomUUID());
  private static final AuthorityId OTHER_ORGANIZATION = new AuthorityId(UUID.randomUUID());

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final List<ShiftHandover> handovers = new ArrayList<>();
  private final List<DomainEvent> events = new ArrayList<>();
  private final Set<AuthorityId> activeAuthorities =
      Set.of(OUTGOING, INCOMING, OTHER_ORGANIZATION);
  private final Map<AuthorityId, OrganizationId> organizations =
      new HashMap<>(
          Map.of(
              OUTGOING, COER,
              INCOMING, COER,
              OTHER_ORGANIZATION, new OrganizationId(UUID.randomUUID())));

  private final AuthorityDirectory directory =
      new AuthorityDirectory() {
        @Override
        public Optional<OrganizationId> organizationOf(AuthorityId authority) {
          return Optional.ofNullable(organizations.get(authority));
        }

        @Override
        public Map<AuthorityId, String> fullNames(Collection<AuthorityId> authorities) {
          return Map.of();
        }
      };
  private final ShiftHandoverRepository handoverRepository =
      handover -> {
        handovers.add(handover);
        return handover;
      };
  private final RegisterZoneCommandHandler registerZone =
      new RegisterZoneCommandHandler(emergencies, zones, events::add);
  private final HandOverShiftCommandHandler handOver =
      new HandOverShiftCommandHandler(
          zones,
          emergencies,
          handoverRepository,
          activeAuthorities::contains,
          directory,
          events::add,
          CLOCK);
  private final ZoneQueryService queries =
      new ZoneQueryService(emergencies, zones, new InMemoryFieldReportRepository());

  private final Emergency emergency = emergencies.save(emergency("Huaico Chosica 2026"));

  @Test
  void theZoneIsInChargeOfTheAuthorityWhoRegisteredIt() {
    Zone zone = zone(emergency, "Sector Alto Perú");

    assertThat(zone.responsibleAuthority()).contains(OUTGOING);
    assertThat(queries.handle(new GetZonesInChargeQuery(OUTGOING))).containsExactly(zone);
  }

  @Test
  void theZonesOfActiveEmergenciesPassToTheIncomingAuthority() {
    Zone altoPeru = zone(emergency, "Sector Alto Perú");
    Zone pedregal = zone(emergency, "Quebrada Pedregal");
    Emergency closed = emergencies.save(emergency("Lluvias Chosica 2025"));
    Zone attended = zone(closed, "Nicolás de Piérola");
    closed.close();
    emergencies.save(closed);
    events.clear();

    ShiftHandover handover =
        handOver.handle(
            new HandOverShiftCommand(COER, OUTGOING, INCOMING, "Alto Perú sin acceso"));

    assertThat(handover.zoneIds()).containsExactly(altoPeru.id(), pedregal.id());
    assertThat(handovers).containsExactly(handover);
    assertThat(zones.findById(altoPeru.id()).orElseThrow().responsibleAuthority())
        .contains(INCOMING);
    assertThat(zones.findById(attended.id()).orElseThrow().responsibleAuthority())
        .contains(OUTGOING);
    assertThat(queries.handle(new GetZonesInChargeQuery(INCOMING)))
        .extracting(Zone::id)
        .containsExactly(altoPeru.id(), pedregal.id());
    assertThat(queries.handle(new GetZonesInChargeQuery(OUTGOING))).isEmpty();
    assertThat(events)
        .containsExactly(
            new ShiftHandedOver(
                handover.id(),
                OUTGOING,
                INCOMING,
                List.of(altoPeru.id(), pedregal.id()),
                "Alto Perú sin acceso",
                LocalDateTime.of(2026, 3, 15, 20, 0)));
  }

  @Test
  void theShiftGoesOnlyToAnotherActiveAuthorityOfTheOrganization() {
    zone(emergency, "Sector Alto Perú");

    assertThatThrownBy(
            () -> handOver.handle(new HandOverShiftCommand(COER, OUTGOING, OUTGOING, null)))
        .isInstanceOf(ShiftHandoverToSelfException.class);
    assertThatThrownBy(
            () ->
                handOver.handle(
                    new HandOverShiftCommand(COER, OUTGOING, OTHER_ORGANIZATION, null)))
        .isInstanceOf(IneligibleIncomingAuthorityException.class);
    var formerAuthority = new AuthorityId(UUID.randomUUID());
    organizations.put(formerAuthority, COER);
    assertThatThrownBy(
            () -> handOver.handle(new HandOverShiftCommand(COER, OUTGOING, formerAuthority, null)))
        .isInstanceOf(IneligibleIncomingAuthorityException.class);
    assertThatThrownBy(
            () -> handOver.handle(new HandOverShiftCommand(COER, formerAuthority, INCOMING, null)))
        .isInstanceOf(ActionNotAllowedException.class);
    assertThat(handovers).isEmpty();
    assertThat(queries.handle(new GetZonesInChargeQuery(OUTGOING))).hasSize(1);
  }

  private Zone zone(Emergency emergency, String name) {
    return registerZone.handle(
        new RegisterZoneCommand(
            emergency.id(),
            name,
            null,
            new PopulationImpact(600, 35, 140, 40, 15, 0),
            DamageLevel.SEVERE,
            OUTGOING));
  }

  private static Emergency emergency(String name) {
    return Emergency.register(
        name,
        EmergencyType.LANDSLIDE,
        null,
        new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
        LocalDate.of(2026, 3, 14));
  }
}
