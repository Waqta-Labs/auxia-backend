package com.waqtalabs.auxia.citizentransparency.domain.model.readmodels;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicStage;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Public stage of attention of a zone and its delivery history (US-24, US-25). It holds no
 * personal data, no evidence and no urgency score (TS-C02, R-04).
 *
 * <p>The events that move it arrive after the commit of their context, so they can arrive late
 * or twice. Within one distribution plan the zone only moves forward: a late {@code
 * DistributionPlanApproved} never takes a delivered zone back to "aid approved". A new plan
 * starts the cycle again from {@code AID_APPROVED}, because a zone can receive several deliveries.
 */
public final class ZonePublicStatus {

  private final ZoneId zoneId;
  private final EmergencyId emergencyId;
  private final String zoneName;
  private PublicStage stage;
  private DistributionPlanId currentDistributionPlanId;
  private DeliveryId currentDeliveryId;
  private int deliveriesCompleted;
  private LocalDateTime lastDeliveryAt;
  private boolean lastDeliveryVerified;

  private ZonePublicStatus(
      ZoneId zoneId,
      EmergencyId emergencyId,
      String zoneName,
      PublicStage stage,
      DistributionPlanId currentDistributionPlanId,
      DeliveryId currentDeliveryId,
      int deliveriesCompleted,
      LocalDateTime lastDeliveryAt,
      boolean lastDeliveryVerified) {
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.emergencyId = Objects.requireNonNull(emergencyId, "emergencyId");
    this.zoneName = Objects.requireNonNull(zoneName, "zoneName");
    this.stage = Objects.requireNonNull(stage, "stage");
    if (deliveriesCompleted < 0) {
      throw new IllegalArgumentException("deliveriesCompleted must not be negative");
    }
    this.currentDistributionPlanId = currentDistributionPlanId;
    this.currentDeliveryId = currentDeliveryId;
    this.deliveriesCompleted = deliveriesCompleted;
    this.lastDeliveryAt = lastDeliveryAt;
    this.lastDeliveryVerified = lastDeliveryVerified;
  }

  /** The view of a zone just registered. */
  public static ZonePublicStatus project(ZoneId zoneId, EmergencyId emergencyId, String zoneName) {
    return new ZonePublicStatus(
        zoneId, emergencyId, zoneName, PublicStage.REGISTERED, null, null, 0, null, false);
  }

  /**
   * Rebuilds a view that is already stored.
   *
   * @param currentDistributionPlanId {@code null} until a plan of the zone is approved
   * @param currentDeliveryId {@code null} until a delivery of the zone starts
   * @param lastDeliveryAt {@code null} until a delivery is completed
   */
  public static ZonePublicStatus reconstitute(
      ZoneId zoneId,
      EmergencyId emergencyId,
      String zoneName,
      PublicStage stage,
      DistributionPlanId currentDistributionPlanId,
      DeliveryId currentDeliveryId,
      int deliveriesCompleted,
      LocalDateTime lastDeliveryAt,
      boolean lastDeliveryVerified) {
    return new ZonePublicStatus(
        zoneId,
        emergencyId,
        zoneName,
        stage,
        currentDistributionPlanId,
        currentDeliveryId,
        deliveriesCompleted,
        lastDeliveryAt,
        lastDeliveryVerified);
  }

  /** The zone has an urgency score. Once aid is on its way, a new score changes nothing. */
  public void markPrioritized() {
    if (stage == PublicStage.REGISTERED) {
      stage = PublicStage.PRIORITIZED;
    }
  }

  public void markAidApproved(DistributionPlanId planId) {
    if (canMoveTo(PublicStage.AID_APPROVED, planId)) {
      currentDistributionPlanId = planId;
      stage = PublicStage.AID_APPROVED;
    }
  }

  public void markAidInTransit(DistributionPlanId planId, DeliveryId deliveryId) {
    Objects.requireNonNull(deliveryId, "deliveryId");
    if (canMoveTo(PublicStage.AID_IN_TRANSIT, planId)) {
      currentDistributionPlanId = planId;
      currentDeliveryId = deliveryId;
      stage = PublicStage.AID_IN_TRANSIT;
    }
  }

  /** Counts the delivery once, even if its event arrives twice. */
  public void markAidDelivered(
      DistributionPlanId planId, DeliveryId deliveryId, LocalDateTime deliveredAt) {
    Objects.requireNonNull(deliveryId, "deliveryId");
    Objects.requireNonNull(deliveredAt, "deliveredAt");
    if (canMoveTo(PublicStage.AID_DELIVERED, planId)) {
      currentDistributionPlanId = planId;
      currentDeliveryId = deliveryId;
      stage = PublicStage.AID_DELIVERED;
      deliveriesCompleted++;
      lastDeliveryAt = deliveredAt;
      lastDeliveryVerified = false;
    }
  }

  /** The hash of the last completed delivery is anchored in the blockchain. */
  public void markDeliveryVerified(DeliveryId deliveryId) {
    if (deliveryId.equals(currentDeliveryId)
        && deliveriesCompleted > 0
        && stage != PublicStage.AID_IN_TRANSIT) {
      lastDeliveryVerified = true;
    }
  }

  /** Within the plan in course only forward; another plan starts the cycle again. */
  private boolean canMoveTo(PublicStage target, DistributionPlanId planId) {
    Objects.requireNonNull(planId, "planId");
    return !planId.equals(currentDistributionPlanId) || target.isAfter(stage);
  }

  /** The verified delivery a citizen can check, while it is the last thing that happened. */
  public Optional<DeliveryId> verifiedDelivery() {
    return stage == PublicStage.AID_DELIVERED && lastDeliveryVerified
        ? Optional.of(currentDeliveryId)
        : Optional.empty();
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public EmergencyId emergencyId() {
    return emergencyId;
  }

  public String zoneName() {
    return zoneName;
  }

  public PublicStage stage() {
    return stage;
  }

  public Optional<DistributionPlanId> currentDistributionPlanId() {
    return Optional.ofNullable(currentDistributionPlanId);
  }

  public Optional<DeliveryId> currentDeliveryId() {
    return Optional.ofNullable(currentDeliveryId);
  }

  public int deliveriesCompleted() {
    return deliveriesCompleted;
  }

  public Optional<LocalDateTime> lastDeliveryAt() {
    return Optional.ofNullable(lastDeliveryAt);
  }

  public boolean lastDeliveryVerified() {
    return lastDeliveryVerified;
  }
}
