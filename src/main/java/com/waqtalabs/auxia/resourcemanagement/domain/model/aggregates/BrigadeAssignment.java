package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Assignment of a brigade to a distribution plan and its zone. Entity inside the {@link Brigade}
 * aggregate, which keeps them as its history.
 */
public final class BrigadeAssignment {

  private final AssignmentId id;
  private final DistributionPlanId distributionPlanId;
  private final ZoneId zoneId;
  private final LocalDateTime assignedAt;
  private AssignmentStatus status;
  private LocalDateTime completedAt;

  private BrigadeAssignment(
      AssignmentId id,
      DistributionPlanId distributionPlanId,
      ZoneId zoneId,
      AssignmentStatus status,
      LocalDateTime assignedAt,
      LocalDateTime completedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.distributionPlanId = Objects.requireNonNull(distributionPlanId, "distributionPlanId");
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.status = Objects.requireNonNull(status, "status");
    this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
    this.completedAt = completedAt;
  }

  static BrigadeAssignment start(DistributionPlanId planId, ZoneId zoneId, LocalDateTime at) {
    return new BrigadeAssignment(
        AssignmentId.generate(), planId, zoneId, AssignmentStatus.IN_PROGRESS, at, null);
  }

  /**
   * Rebuilds an assignment that is already stored.
   *
   * @param completedAt {@code null} while the assignment is in progress
   */
  public static BrigadeAssignment reconstitute(
      AssignmentId id,
      DistributionPlanId distributionPlanId,
      ZoneId zoneId,
      AssignmentStatus status,
      LocalDateTime assignedAt,
      LocalDateTime completedAt) {
    return new BrigadeAssignment(id, distributionPlanId, zoneId, status, assignedAt, completedAt);
  }

  void complete(LocalDateTime at) {
    status = AssignmentStatus.COMPLETED;
    completedAt = Objects.requireNonNull(at, "at");
  }

  public boolean isInProgress() {
    return status == AssignmentStatus.IN_PROGRESS;
  }

  public boolean isFor(DistributionPlanId planId) {
    return distributionPlanId.equals(planId);
  }

  public AssignmentId id() {
    return id;
  }

  public DistributionPlanId distributionPlanId() {
    return distributionPlanId;
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public AssignmentStatus status() {
    return status;
  }

  public LocalDateTime assignedAt() {
    return assignedAt;
  }

  /** Empty while the assignment is in progress. */
  public Optional<LocalDateTime> completedAt() {
    return Optional.ofNullable(completedAt);
  }
}
