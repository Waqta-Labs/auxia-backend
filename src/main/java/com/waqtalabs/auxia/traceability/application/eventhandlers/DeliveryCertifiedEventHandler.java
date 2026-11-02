package com.waqtalabs.auxia.traceability.application.eventhandlers;

import com.waqtalabs.auxia.traceability.application.ports.DeliveryCertificationNotifier;
import com.waqtalabs.auxia.traceability.application.ports.DeliveryCertificationNotifier.CertifiedDelivery;
import com.waqtalabs.auxia.traceability.application.ports.ZoneNamePort;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryCertified;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies a certified delivery once its anchoring is committed. A notification that fails is
 * logged: the delivery stays certified.
 */
@Component
public class DeliveryCertifiedEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DeliveryCertifiedEventHandler.class);

  private final DeliveryRepository deliveryRepository;
  private final ZoneNamePort zoneNames;
  private final DeliveryCertificationNotifier notifier;

  public DeliveryCertifiedEventHandler(
      DeliveryRepository deliveryRepository,
      ZoneNamePort zoneNames,
      DeliveryCertificationNotifier notifier) {
    this.deliveryRepository = deliveryRepository;
    this.zoneNames = zoneNames;
    this.notifier = notifier;
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(DeliveryCertified event) {
    try {
      Delivery delivery = deliveryRepository.findById(event.deliveryId()).orElseThrow();
      String zoneName =
          zoneNames.zoneNames(List.of(delivery.zoneId())).getOrDefault(delivery.zoneId(), "");
      notifier.notifyCertified(
          new CertifiedDelivery(
              delivery.id(),
              delivery.organizationId(),
              zoneName,
              delivery.registration().orElseThrow().deliveredAt(),
              event.transactionHash()));
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Certification of delivery {} was not notified", event.deliveryId().value(), ex);
    }
  }
}
