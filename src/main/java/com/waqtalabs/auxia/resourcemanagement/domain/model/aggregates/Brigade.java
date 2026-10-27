package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.AssignmentNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeAssignedException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotAvailableException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeWithoutLeaderException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InvalidBrigadeAvailabilityException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.StaffMemberNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Field brigade of an organization: the unit that delivers a distribution plan (Staff aggregate).
 *
 * <p>A brigade has at most one assignment in progress, and it can be assigned only while it is
 * available and has a leader. Its availability changes by hand only between {@code AVAILABLE} and
 * {@code OFF_DUTY}; it becomes {@code ASSIGNED} with a plan and available again when the delivery
 * is confirmed.
 */
public final class Brigade {

  private final BrigadeId id;
  private final OrganizationId organizationId;
  private final String name;
  private final Location baseLocation;
  private final List<StaffMember> members;
  private final List<BrigadeAssignment> assignments;
  private BrigadeStatus status;

  private Brigade(
      BrigadeId id,
      OrganizationId organizationId,
      String name,
      Location baseLocation,
      BrigadeStatus status,
      List<StaffMember> members,
      List<BrigadeAssignment> assignments) {
    this.id = Objects.requireNonNull(id, "id");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.name = name.strip();
    this.baseLocation = Objects.requireNonNull(baseLocation, "baseLocation");
    this.status = Objects.requireNonNull(status, "status");
    this.members = new ArrayList<>(members);
    this.assignments = new ArrayList<>(assignments);
  }

  /** Creates an available brigade, still without members. */
  public static Brigade create(OrganizationId organizationId, String name, Location baseLocation) {
    return new Brigade(
        BrigadeId.generate(),
        organizationId,
        name,
        baseLocation,
        BrigadeStatus.AVAILABLE,
        List.of(),
        List.of());
  }

  /** Rebuilds a brigade that is already stored. */
  public static Brigade reconstitute(
      BrigadeId id,
      OrganizationId organizationId,
      String name,
      Location baseLocation,
      BrigadeStatus status,
      List<StaffMember> members,
      List<BrigadeAssignment> assignments) {
    return new Brigade(id, organizationId, name, baseLocation, status, members, assignments);
  }

  public void addMember(StaffMember member) {
    members.add(Objects.requireNonNull(member, "member"));
  }

  /**
   * Removes a member.
   *
   * @throws StaffMemberNotFoundException if the member is not in this brigade
   * @throws BrigadeWithoutLeaderException if the brigade is assigned and it is its last leader
   */
  public void removeMember(StaffMemberId memberId) {
    StaffMember member =
        members.stream()
            .filter(candidate -> candidate.id().equals(memberId))
            .findFirst()
            .orElseThrow(() -> new StaffMemberNotFoundException(memberId, id));
    if (status == BrigadeStatus.ASSIGNED && member.isLeader() && leaders() == 1) {
      throw new BrigadeWithoutLeaderException(id);
    }
    members.remove(member);
  }

  /**
   * Assigns the brigade to a distribution plan and moves it to {@code ASSIGNED}.
   *
   * @throws BrigadeNotAvailableException if the brigade is assigned or off duty
   * @throws BrigadeWithoutLeaderException if no member has the {@code LEADER} role
   */
  public void assignTo(DistributionPlanId planId, ZoneId zoneId, LocalDateTime assignedAt) {
    if (!isAvailable()) {
      throw new BrigadeNotAvailableException(id, status);
    }
    if (leaders() == 0) {
      throw new BrigadeWithoutLeaderException(id);
    }
    assignments.add(BrigadeAssignment.start(planId, zoneId, assignedAt));
    status = BrigadeStatus.ASSIGNED;
  }

  /**
   * Closes the assignment of a plan once its delivery is confirmed, and makes the brigade
   * available again.
   *
   * @throws AssignmentNotFoundException if the plan has no assignment in progress here
   */
  public void completeAssignment(DistributionPlanId planId, LocalDateTime completedAt) {
    currentAssignment()
        .filter(assignment -> assignment.isFor(planId))
        .orElseThrow(() -> new AssignmentNotFoundException(id, planId))
        .complete(completedAt);
    status = BrigadeStatus.AVAILABLE;
  }

  /**
   * Changes between {@code AVAILABLE} and {@code OFF_DUTY}.
   *
   * @throws BrigadeAssignedException if the brigade has an assignment in progress
   * @throws InvalidBrigadeAvailabilityException if the new status is {@code ASSIGNED}
   */
  public void changeAvailability(BrigadeStatus newStatus) {
    Objects.requireNonNull(newStatus, "newStatus");
    if (newStatus == BrigadeStatus.ASSIGNED) {
      throw new InvalidBrigadeAvailabilityException(newStatus);
    }
    if (status == BrigadeStatus.ASSIGNED) {
      throw new BrigadeAssignedException(id);
    }
    status = newStatus;
  }

  public boolean isAvailable() {
    return status == BrigadeStatus.AVAILABLE;
  }

  public boolean hasLeader() {
    return leaders() > 0;
  }

  public Optional<BrigadeAssignment> currentAssignment() {
    return assignments.stream().filter(BrigadeAssignment::isInProgress).findFirst();
  }

  public boolean belongsTo(OrganizationId organizationId) {
    return this.organizationId.equals(organizationId);
  }

  public boolean hasName(String other) {
    return name.equalsIgnoreCase(other.strip());
  }

  public BrigadeId id() {
    return id;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public String name() {
    return name;
  }

  public Location baseLocation() {
    return baseLocation;
  }

  public BrigadeStatus status() {
    return status;
  }

  public List<StaffMember> members() {
    return List.copyOf(members);
  }

  /** History of assignments, the one in progress included. */
  public List<BrigadeAssignment> assignments() {
    return List.copyOf(assignments);
  }

  private long leaders() {
    return members.stream().filter(StaffMember::isLeader).count();
  }
}
