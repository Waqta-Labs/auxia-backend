package com.waqtalabs.auxia.resourcemanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AssignBrigadeCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.CompleteBrigadeAssignmentCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.AssignBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.CompleteBrigadeAssignmentCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.BrigadeAssigned;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.AssignmentNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.DistributionPlanAlreadyAssignedException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.PlanAssignmentNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BrigadeAssignmentUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-14T14:30:00Z"), ZoneOffset.ofHours(-5));
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  private static final Location ZONE = new Location(-11.9436, -76.7078);
  private static final Location NEAR = new Location(-11.9500, -76.7100);
  private static final Location FAR = new Location(-12.0464, -77.0428);

  private final InMemoryBrigadeRepository brigades = new InMemoryBrigadeRepository();
  private final FakeAiService aiService = new FakeAiService();
  private final List<DomainEvent> events = new ArrayList<>();
  private final AssignBrigadeCommandHandler assign =
      new AssignBrigadeCommandHandler(brigades, aiService, events::add, CLOCK);
  private final CompleteBrigadeAssignmentCommandHandler complete =
      new CompleteBrigadeAssignmentCommandHandler(brigades, CLOCK);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final DistributionPlanId plan = new DistributionPlanId(UUID.randomUUID());
  private final ZoneId zone = new ZoneId(UUID.randomUUID());

  @Test
  void closestAvailableBrigadeWithALeaderGetsThePlan() {
    Brigade far = brigade("Brigada A", FAR, true);
    Brigade near = brigade("Brigada B", NEAR, true);

    Optional<Brigade> assigned = assign.handle(command(Optional.of(ZONE)));

    assertThat(assigned).map(Brigade::id).contains(near.id());
    assertThat(brigades.findById(near.id()).orElseThrow().status())
        .isEqualTo(BrigadeStatus.ASSIGNED);
    assertThat(brigades.findById(far.id()).orElseThrow().status())
        .isEqualTo(BrigadeStatus.AVAILABLE);
    assertThat(events)
        .containsExactly(new BrigadeAssigned(near.id(), plan, zone, organization, NOW));
  }

  @Test
  void brigadesThatCannotTakeThePlanAreNotCandidates() {
    brigade("Brigada sin líder", NEAR, false);
    Brigade offDuty = brigade("Brigada fuera de turno", NEAR, true);
    offDuty.changeAvailability(BrigadeStatus.OFF_DUTY);
    brigades.save(offDuty);
    Brigade other = Brigade.create(new OrganizationId(UUID.randomUUID()), "Otra", NEAR);
    other.addMember(StaffMember.register("Rosa Quispe", StaffRole.LEADER, null));
    brigades.save(other);
    Brigade candidate = brigade("Brigada lejana", FAR, true);

    assertThat(assign.handle(command(Optional.of(ZONE)))).map(Brigade::id).contains(candidate.id());
  }

  @Test
  void withoutCandidatesNothingIsAssigned() {
    brigade("Brigada sin líder", NEAR, false);

    assertThat(assign.handle(command(Optional.of(ZONE)))).isEmpty();
    assertThat(events).isEmpty();
  }

  @Test
  void zoneWithoutCoordinatesGetsTheFirstCandidateByName() {
    brigade("Brigada B", NEAR, true);
    Brigade first = brigade("Brigada A", FAR, true);
    aiService.becomeUnavailable();

    assertThat(assign.handle(command(Optional.empty()))).map(Brigade::id).contains(first.id());
  }

  @Test
  void planIsAssignedOnlyOnce() {
    Brigade assigned = brigade("Brigada A", NEAR, true);
    brigade("Brigada B", FAR, true);
    assign.handle(command(Optional.of(ZONE)));

    assertThatThrownBy(() -> assign.handle(command(Optional.of(ZONE))))
        .isInstanceOf(DistributionPlanAlreadyAssignedException.class)
        .hasMessageContaining(assigned.id().value().toString());
  }

  @Test
  void unavailableAiServiceAssignsNothing() {
    Brigade brigade = brigade("Brigada A", NEAR, true);
    aiService.becomeUnavailable();

    assertThatThrownBy(() -> assign.handle(command(Optional.of(ZONE))))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    assertThat(brigades.findById(brigade.id()).orElseThrow().isAvailable()).isTrue();
  }

  @Test
  void completedAssignmentMakesTheBrigadeAvailableAgain() {
    Brigade brigade = brigade("Brigada A", NEAR, true);
    assign.handle(command(Optional.of(ZONE)));

    complete.handle(new CompleteBrigadeAssignmentCommand(plan));

    Brigade completed = brigades.findById(brigade.id()).orElseThrow();
    assertThat(completed.isAvailable()).isTrue();
    assertThat(completed.assignments().getFirst().completedAt()).contains(NOW);
    assertThatThrownBy(() -> complete.handle(new CompleteBrigadeAssignmentCommand(plan)))
        .isInstanceOf(AssignmentNotFoundException.class);
  }

  @Test
  void planWithoutBrigadeCannotBeCompleted() {
    var unassigned = new CompleteBrigadeAssignmentCommand(plan);

    assertThatThrownBy(() -> complete.handle(unassigned))
        .isInstanceOf(PlanAssignmentNotFoundException.class);
  }

  private AssignBrigadeCommand command(Optional<Location> zoneLocation) {
    return new AssignBrigadeCommand(organization, plan, zone, zoneLocation);
  }

  private Brigade brigade(String name, Location base, boolean withLeader) {
    Brigade brigade = Brigade.create(organization, name, base);
    if (withLeader) {
      brigade.addMember(StaffMember.register("Rosa Quispe", StaffRole.LEADER, null));
    }
    return brigades.save(brigade);
  }
}
