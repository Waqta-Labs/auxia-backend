package com.waqtalabs.auxia.citizentransparency.domain.model.readmodels;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicStage;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZonePublicStatusTests {

  private static final LocalDateTime MORNING = LocalDateTime.of(2026, 3, 15, 10, 30);

  private final DistributionPlanId plan = new DistributionPlanId(UUID.randomUUID());
  private final DeliveryId delivery = new DeliveryId(UUID.randomUUID());
  private final ZonePublicStatus status =
      ZonePublicStatus.project(
          new ZoneId(UUID.randomUUID()), new EmergencyId(UUID.randomUUID()), "Sector Alto Perú");

  @Test
  void zoneMovesThroughEveryStageOfItsPlan() {
    assertThat(status.stage()).isEqualTo(PublicStage.REGISTERED);

    status.markPrioritized();
    assertThat(status.stage()).isEqualTo(PublicStage.PRIORITIZED);
    status.markAidApproved(plan);
    assertThat(status.stage()).isEqualTo(PublicStage.AID_APPROVED);
    status.markAidInTransit(plan, delivery);
    assertThat(status.stage()).isEqualTo(PublicStage.AID_IN_TRANSIT);
    status.markAidDelivered(plan, delivery, MORNING);

    assertThat(status.stage()).isEqualTo(PublicStage.AID_DELIVERED);
    assertThat(status.deliveriesCompleted()).isEqualTo(1);
    assertThat(status.lastDeliveryAt()).contains(MORNING);
    assertThat(status.lastDeliveryVerified()).isFalse();
    assertThat(status.verifiedDelivery()).isEmpty();
  }

  @Test
  void lateEventsNeverTakeTheZoneBackWithinItsPlan() {
    status.markAidApproved(plan);
    status.markAidInTransit(plan, delivery);
    status.markAidDelivered(plan, delivery, MORNING);

    status.markAidApproved(plan);
    status.markAidInTransit(plan, delivery);
    status.markPrioritized();

    assertThat(status.stage()).isEqualTo(PublicStage.AID_DELIVERED);
  }

  @Test
  void deliveryArrivingTwiceIsCountedOnce() {
    status.markAidInTransit(plan, delivery);
    status.markAidDelivered(plan, delivery, MORNING);

    status.markAidDelivered(plan, delivery, MORNING.plusHours(1));

    assertThat(status.deliveriesCompleted()).isEqualTo(1);
    assertThat(status.lastDeliveryAt()).contains(MORNING);
  }

  @Test
  void transitBeforeTheApprovalEventStillMovesForward() {
    status.markPrioritized();
    status.markAidInTransit(plan, delivery);

    status.markAidApproved(plan);

    assertThat(status.stage()).isEqualTo(PublicStage.AID_IN_TRANSIT);
    assertThat(status.currentDistributionPlanId()).contains(plan);
  }

  @Test
  void newPlanStartsTheCycleAgainAndKeepsTheHistory() {
    status.markAidInTransit(plan, delivery);
    status.markAidDelivered(plan, delivery, MORNING);
    status.markDeliveryVerified(delivery);
    var next = new DistributionPlanId(UUID.randomUUID());
    var nextDelivery = new DeliveryId(UUID.randomUUID());

    status.markAidApproved(next);
    assertThat(status.stage()).isEqualTo(PublicStage.AID_APPROVED);
    assertThat(status.lastDeliveryVerified()).isTrue();
    assertThat(status.verifiedDelivery()).isEmpty();

    status.markAidInTransit(next, nextDelivery);
    status.markAidDelivered(next, nextDelivery, MORNING.plusDays(2));

    assertThat(status.deliveriesCompleted()).isEqualTo(2);
    assertThat(status.lastDeliveryAt()).contains(MORNING.plusDays(2));
    assertThat(status.lastDeliveryVerified()).isFalse();
  }

  @Test
  void onlyTheLastCompletedDeliveryIsVerified() {
    status.markAidInTransit(plan, delivery);
    status.markDeliveryVerified(delivery);
    assertThat(status.lastDeliveryVerified()).isFalse();

    status.markAidDelivered(plan, delivery, MORNING);
    status.markDeliveryVerified(new DeliveryId(UUID.randomUUID()));
    assertThat(status.lastDeliveryVerified()).isFalse();

    status.markDeliveryVerified(delivery);
    assertThat(status.lastDeliveryVerified()).isTrue();
    assertThat(status.verifiedDelivery()).contains(delivery);
  }

  @Test
  void newScoreChangesNothingOnceAidIsApproved() {
    status.markAidApproved(plan);

    status.markPrioritized();

    assertThat(status.stage()).isEqualTo(PublicStage.AID_APPROVED);
  }
}
