package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.AssignmentNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeAssignedException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotAvailableException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeWithoutLeaderException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InvalidBrigadeAvailabilityException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.StaffMemberNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BrigadeTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final Location CHOSICA = new Location(-11.9436, -76.7078);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final DistributionPlanId plan = new DistributionPlanId(UUID.randomUUID());
  private final ZoneId zone = new ZoneId(UUID.randomUUID());
  private final Brigade brigade = Brigade.create(organization, " Brigada Chosica 1 ", CHOSICA);
  private final StaffMember leader =
      StaffMember.register("Rosa Quispe", StaffRole.LEADER, "987654321");

  @Test
  void newBrigadeIsAvailableWithoutMembers() {
    assertThat(brigade.name()).isEqualTo("Brigada Chosica 1");
    assertThat(brigade.belongsTo(organization)).isTrue();
    assertThat(brigade.baseLocation()).isEqualTo(CHOSICA);
    assertThat(brigade.status()).isEqualTo(BrigadeStatus.AVAILABLE);
    assertThat(brigade.members()).isEmpty();
    assertThat(brigade.hasLeader()).isFalse();
  }

  @Test
  void membersAreAddedAndRemoved() {
    StaffMember driver = StaffMember.register("Luis Huamán", StaffRole.DRIVER, " ");
    brigade.addMember(leader);
    brigade.addMember(driver);

    brigade.removeMember(driver.id());

    assertThat(brigade.members()).containsExactly(leader);
    assertThat(brigade.hasLeader()).isTrue();
    assertThat(driver.phone()).isEmpty();
    assertThatThrownBy(() -> brigade.removeMember(StaffMemberId.generate()))
        .isInstanceOf(StaffMemberNotFoundException.class);
  }

  @Test
  void availableBrigadeWithALeaderIsAssignedToAPlan() {
    brigade.addMember(leader);

    brigade.assignTo(plan, zone, NOW);

    assertThat(brigade.status()).isEqualTo(BrigadeStatus.ASSIGNED);
    assertThat(brigade.isAvailable()).isFalse();
    BrigadeAssignment assignment = brigade.currentAssignment().orElseThrow();
    assertThat(assignment.distributionPlanId()).isEqualTo(plan);
    assertThat(assignment.zoneId()).isEqualTo(zone);
    assertThat(assignment.assignedAt()).isEqualTo(NOW);
    assertThat(assignment.status()).isEqualTo(AssignmentStatus.IN_PROGRESS);
  }

  @Test
  void brigadeWithoutLeaderCannotBeAssigned() {
    brigade.addMember(StaffMember.register("Luis Huamán", StaffRole.DRIVER, null));

    assertThatThrownBy(() -> brigade.assignTo(plan, zone, NOW))
        .isInstanceOf(BrigadeWithoutLeaderException.class);
    assertThat(brigade.isAvailable()).isTrue();
  }

  @Test
  void brigadeHasOneAssignmentInProgressAtMost() {
    brigade.addMember(leader);
    brigade.assignTo(plan, zone, NOW);

    var otherPlan = new DistributionPlanId(UUID.randomUUID());
    assertThatThrownBy(() -> brigade.assignTo(otherPlan, zone, NOW))
        .isInstanceOf(BrigadeNotAvailableException.class);
  }

  @Test
  void offDutyBrigadeCannotBeAssigned() {
    brigade.addMember(leader);
    brigade.changeAvailability(BrigadeStatus.OFF_DUTY);

    assertThatThrownBy(() -> brigade.assignTo(plan, zone, NOW))
        .isInstanceOf(BrigadeNotAvailableException.class);
  }

  @Test
  void completingTheAssignmentMakesTheBrigadeAvailableAgain() {
    brigade.addMember(leader);
    brigade.assignTo(plan, zone, NOW);

    brigade.completeAssignment(plan, NOW.plusHours(5));

    assertThat(brigade.status()).isEqualTo(BrigadeStatus.AVAILABLE);
    assertThat(brigade.currentAssignment()).isEmpty();
    BrigadeAssignment done = brigade.assignments().getFirst();
    assertThat(done.status()).isEqualTo(AssignmentStatus.COMPLETED);
    assertThat(done.completedAt()).contains(NOW.plusHours(5));
  }

  @Test
  void onlyThePlanInProgressCanBeCompleted() {
    brigade.addMember(leader);
    brigade.assignTo(plan, zone, NOW);

    var otherPlan = new DistributionPlanId(UUID.randomUUID());
    assertThatThrownBy(() -> brigade.completeAssignment(otherPlan, NOW))
        .isInstanceOf(AssignmentNotFoundException.class);
  }

  @Test
  void lastLeaderOfAnAssignedBrigadeCannotBeRemoved() {
    brigade.addMember(leader);
    brigade.assignTo(plan, zone, NOW);

    assertThatThrownBy(() -> brigade.removeMember(leader.id()))
        .isInstanceOf(BrigadeWithoutLeaderException.class);
  }

  @Test
  void availabilityChangesOnlyBetweenAvailableAndOffDutyWithoutAssignment() {
    brigade.changeAvailability(BrigadeStatus.OFF_DUTY);
    assertThat(brigade.status()).isEqualTo(BrigadeStatus.OFF_DUTY);
    brigade.changeAvailability(BrigadeStatus.AVAILABLE);
    assertThatThrownBy(() -> brigade.changeAvailability(BrigadeStatus.ASSIGNED))
        .isInstanceOf(InvalidBrigadeAvailabilityException.class);

    brigade.addMember(leader);
    brigade.assignTo(plan, zone, NOW);
    assertThatThrownBy(() -> brigade.changeAvailability(BrigadeStatus.OFF_DUTY))
        .isInstanceOf(BrigadeAssignedException.class);
  }

  @Test
  void distanceBetweenLocationsIsInKilometers() {
    var lima = new Location(-12.0464, -77.0428);

    // Lima to Chosica: about 37 km in a straight line.
    assertThat(CHOSICA.distanceTo(lima)).isBetween(35.0, 40.0);
    assertThat(CHOSICA.distanceTo(CHOSICA)).isZero();
    assertThatThrownBy(() -> new Location(91, 0)).isInstanceOf(IllegalArgumentException.class);
  }
}
