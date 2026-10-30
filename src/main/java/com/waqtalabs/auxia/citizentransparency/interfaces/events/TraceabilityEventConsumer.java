package com.waqtalabs.auxia.citizentransparency.interfaces.events;

import com.waqtalabs.auxia.citizentransparency.application.projectionhandlers.DeliveryProjectionHandler;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.interfaces.acl.DeliveryCertifiedIntegrationEvent;
import com.waqtalabs.auxia.traceability.interfaces.acl.DeliveryConfirmedIntegrationEvent;
import com.waqtalabs.auxia.traceability.interfaces.acl.DeliveryInitiatedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Receives the public events of Traceability once their transaction is committed, and projects
 * each one in a transaction of its own. A confirmed delivery is projected before the anchoring
 * starts, so its certification always finds it delivered.
 */
@Component
class TraceabilityEventConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TraceabilityEventConsumer.class);

  private final DeliveryProjectionHandler projection;
  private final TransactionTemplate newTransaction;

  TraceabilityEventConsumer(
      DeliveryProjectionHandler projection, PlatformTransactionManager transactionManager) {
    this.projection = projection;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(DeliveryInitiatedIntegrationEvent event) {
    run(
        event.zoneId().toString(),
        () ->
            projection.markAidInTransit(
                new ZoneId(event.zoneId()),
                new DistributionPlanId(event.distributionPlanId()),
                new DeliveryId(event.deliveryId())));
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  @TransactionalEventListener(fallbackExecution = true)
  void on(DeliveryConfirmedIntegrationEvent event) {
    run(
        event.zoneId().toString(),
        () ->
            projection.markAidDelivered(
                new ZoneId(event.zoneId()),
                new DistributionPlanId(event.distributionPlanId()),
                new DeliveryId(event.deliveryId()),
                event.deliveredAt()));
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(DeliveryCertifiedIntegrationEvent event) {
    run(
        event.zoneId().toString(),
        () ->
            projection.markDeliveryVerified(
                new ZoneId(event.zoneId()), new DeliveryId(event.deliveryId())));
  }

  private void run(String zone, Runnable step) {
    try {
      newTransaction.executeWithoutResult(status -> step.run());
    } catch (RuntimeException ex) {
      LOGGER.error("The public view of zone {} was not updated", zone, ex);
    }
  }
}
