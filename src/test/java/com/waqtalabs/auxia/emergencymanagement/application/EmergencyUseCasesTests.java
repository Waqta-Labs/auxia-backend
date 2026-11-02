package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.CloseEmergencyCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterEmergencyCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.CloseEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetActiveEmergenciesQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetEmergencyByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.EmergencyQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.events.EmergencyRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyAlreadyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmergencyUseCasesTests {

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final List<DomainEvent> events = new ArrayList<>();
  private final RegisterEmergencyCommandHandler register =
      new RegisterEmergencyCommandHandler(emergencies, events::add);
  private final CloseEmergencyCommandHandler close = new CloseEmergencyCommandHandler(emergencies);
  private final EmergencyQueryService queries = new EmergencyQueryService(emergencies);

  @Test
  void registeredEmergencyIsStoredAsActive() {
    Emergency emergency = register.handle(command("Huaico Chosica 2026"));

    assertThat(emergencies.findById(emergency.id())).contains(emergency);
    assertThat(emergency.status()).isEqualTo(EmergencyStatus.ACTIVE);
    assertThat(events)
        .containsExactly(
            new EmergencyRegistered(
                emergency.id(),
                "Huaico Chosica 2026",
                emergency.type(),
                emergency.location(),
                emergency.startDate()));
  }

  @Test
  void closedEmergencyIsStoredAsClosed() {
    Emergency emergency = register.handle(command("Huaico Chosica 2026"));

    close.handle(new CloseEmergencyCommand(emergency.id()));

    assertThat(emergencies.findById(emergency.id()).orElseThrow().isActive()).isFalse();
  }

  @Test
  void closingAnUnknownEmergencyIsRejected() {
    var command = new CloseEmergencyCommand(EmergencyId.generate());

    assertThatThrownBy(() -> close.handle(command))
        .isInstanceOf(EmergencyNotFoundException.class);
  }

  @Test
  void closingTwiceIsRejected() {
    Emergency emergency = register.handle(command("Huaico Chosica 2026"));
    close.handle(new CloseEmergencyCommand(emergency.id()));

    assertThatThrownBy(() -> close.handle(new CloseEmergencyCommand(emergency.id())))
        .isInstanceOf(EmergencyAlreadyClosedException.class);
  }

  @Test
  void queriesFindAnEmergencyAndListOnlyTheActiveOnes() {
    Emergency active = register.handle(command("Inundación Piura 2026"));
    Emergency closed = register.handle(command("Incendio Cusco 2026"));
    close.handle(new CloseEmergencyCommand(closed.id()));

    assertThat(queries.handle(new GetEmergencyByIdQuery(closed.id()))).isEqualTo(closed);
    assertThat(queries.handle(new GetActiveEmergenciesQuery())).containsExactly(active);
    assertThatThrownBy(() -> queries.handle(new GetEmergencyByIdQuery(EmergencyId.generate())))
        .isInstanceOf(EmergencyNotFoundException.class);
  }

  private static RegisterEmergencyCommand command(String name) {
    return new RegisterEmergencyCommand(
        name,
        EmergencyType.FLOOD,
        null,
        new AdministrativeLocation("Piura", "Piura", "Castilla"),
        LocalDate.of(2026, 3, 1));
  }
}
