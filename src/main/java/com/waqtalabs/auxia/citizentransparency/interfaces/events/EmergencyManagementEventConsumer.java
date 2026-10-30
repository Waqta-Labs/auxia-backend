package com.waqtalabs.auxia.citizentransparency.interfaces.events;

import com.waqtalabs.auxia.citizentransparency.application.projectionhandlers.EmergencyProjectionHandler;
import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.EmergencyPublicView;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicLocation;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.DistributionPlanApprovedIntegrationEvent;
import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.EmergencyRegisteredIntegrationEvent;
import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.ZonePrioritizedIntegrationEvent;
import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.ZoneRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Receives the public events of Emergency Management once their transaction is committed, and
 * projects each one in a transaction of its own: a failure here never undoes the command.
 */
@Component
class EmergencyManagementEventConsumer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(EmergencyManagementEventConsumer.class);

  private final EmergencyProjectionHandler projection;
  private final TransactionTemplate newTransaction;

  EmergencyManagementEventConsumer(
      EmergencyProjectionHandler projection, PlatformTransactionManager transactionManager) {
    this.projection = projection;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(EmergencyRegisteredIntegrationEvent event) {
    run(
        "emergency " + event.emergencyId(),
        () ->
            projection.projectEmergency(
                EmergencyPublicView.project(
                    new EmergencyId(event.emergencyId()),
                    event.name(),
                    event.type(),
                    new PublicLocation(event.department(), event.province(), event.district()),
                    event.startDate())));
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(ZoneRegisteredIntegrationEvent event) {
    run(
        "zone " + event.zoneId(),
        () ->
            projection.projectZone(
                new ZoneId(event.zoneId()), new EmergencyId(event.emergencyId()), event.name()));
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(ZonePrioritizedIntegrationEvent event) {
    run(
        "zone " + event.zoneId(),
        () -> projection.markPrioritized(new ZoneId(event.zoneId())));
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(DistributionPlanApprovedIntegrationEvent event) {
    run(
        "zone " + event.zoneId(),
        () ->
            projection.markAidApproved(
                new ZoneId(event.zoneId()), new DistributionPlanId(event.distributionPlanId())));
  }

  private void run(String what, Runnable step) {
    try {
      newTransaction.executeWithoutResult(status -> step.run());
    } catch (RuntimeException ex) {
      LOGGER.error("The public view of {} was not updated", what, ex);
    }
  }
}
