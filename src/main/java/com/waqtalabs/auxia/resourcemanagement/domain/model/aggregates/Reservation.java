package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stock held for a distribution plan while it is decided. Entity inside the {@link Inventory}
 * aggregate. It stays {@code ACTIVE} until it is consumed by a delivery or released.
 */
public final class Reservation {

  private final ReservationId id;
  private final DistributionPlanId distributionPlanId;
  private List<ReservationLine> lines;
  private final LocalDateTime reservedAt;
  private ReservationStatus status;
  private LocalDateTime closedAt;

  private Reservation(
      ReservationId id,
      DistributionPlanId distributionPlanId,
      List<ReservationLine> lines,
      ReservationStatus status,
      LocalDateTime reservedAt,
      LocalDateTime closedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.distributionPlanId = Objects.requireNonNull(distributionPlanId, "distributionPlanId");
    this.lines = requireLines(lines);
    this.status = Objects.requireNonNull(status, "status");
    this.reservedAt = Objects.requireNonNull(reservedAt, "reservedAt");
    this.closedAt = closedAt;
  }

  static Reservation open(
      DistributionPlanId distributionPlanId, List<ReservationLine> lines, LocalDateTime at) {
    return new Reservation(
        ReservationId.generate(), distributionPlanId, lines, ReservationStatus.ACTIVE, at, null);
  }

  /**
   * Rebuilds a reservation that is already stored.
   *
   * @param closedAt {@code null} while the reservation is active
   */
  public static Reservation reconstitute(
      ReservationId id,
      DistributionPlanId distributionPlanId,
      List<ReservationLine> lines,
      ReservationStatus status,
      LocalDateTime reservedAt,
      LocalDateTime closedAt) {
    return new Reservation(id, distributionPlanId, lines, status, reservedAt, closedAt);
  }

  void replaceLines(List<ReservationLine> lines) {
    this.lines = requireLines(lines);
  }

  void release(LocalDateTime at) {
    status = ReservationStatus.RELEASED;
    closedAt = Objects.requireNonNull(at, "at");
  }

  public boolean isActive() {
    return status == ReservationStatus.ACTIVE;
  }

  public boolean isFor(DistributionPlanId planId) {
    return distributionPlanId.equals(planId);
  }

  public ReservationId id() {
    return id;
  }

  public DistributionPlanId distributionPlanId() {
    return distributionPlanId;
  }

  public List<ReservationLine> lines() {
    return lines;
  }

  public ReservationStatus status() {
    return status;
  }

  public LocalDateTime reservedAt() {
    return reservedAt;
  }

  /** Empty while the reservation is active. */
  public Optional<LocalDateTime> closedAt() {
    return Optional.ofNullable(closedAt);
  }

  private static List<ReservationLine> requireLines(List<ReservationLine> lines) {
    List<ReservationLine> copy = List.copyOf(lines);
    if (copy.isEmpty()) {
      throw new IllegalArgumentException("a reservation has at least one line");
    }
    return copy;
  }
}
