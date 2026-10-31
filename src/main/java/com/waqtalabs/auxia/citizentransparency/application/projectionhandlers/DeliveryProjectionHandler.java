package com.waqtalabs.auxia.citizentransparency.application.projectionhandlers;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.ZonePublicStatus;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.ZonePublicStatusRepository;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projects the deliveries of Traceability on the public status of their zone (US-25). */
@Service
public class DeliveryProjectionHandler {

  private final ZonePublicStatusRepository zoneStatuses;

  public DeliveryProjectionHandler(ZonePublicStatusRepository zoneStatuses) {
    this.zoneStatuses = zoneStatuses;
  }

  @Transactional
  public void markAidInTransit(ZoneId zoneId, DistributionPlanId planId, DeliveryId deliveryId) {
    update(zoneId, status -> status.markAidInTransit(planId, deliveryId));
  }

  @Transactional
  public void markAidDelivered(
      ZoneId zoneId, DistributionPlanId planId, DeliveryId deliveryId, LocalDateTime deliveredAt) {
    update(zoneId, status -> status.markAidDelivered(planId, deliveryId, deliveredAt));
  }

  @Transactional
  public void markDeliveryVerified(ZoneId zoneId, DeliveryId deliveryId) {
    update(zoneId, status -> status.markDeliveryVerified(deliveryId));
  }

  private void update(ZoneId zoneId, Consumer<ZonePublicStatus> change) {
    zoneStatuses
        .findByZoneId(zoneId)
        .ifPresent(
            status -> {
              change.accept(status);
              zoneStatuses.save(status);
            });
  }
}
