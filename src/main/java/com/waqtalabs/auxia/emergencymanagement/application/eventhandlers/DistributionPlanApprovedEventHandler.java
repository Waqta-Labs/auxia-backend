package com.waqtalabs.auxia.emergencymanagement.application.eventhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AssignDistributionPlanBrigadeCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignDistributionPlanBrigadeCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoAvailableBrigadeException;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gets a brigade for every plan once its approval is committed.
 *
 * <p>It runs in a transaction of its own, so the approval stands even if no brigade is free or
 * the AI Service does not answer: the plan stays approved without a brigade, and an authority
 * asks for one again with {@code POST /api/v1/distribution-plans/{id}/brigade-assignment}.
 */
@Component
public class DistributionPlanApprovedEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DistributionPlanApprovedEventHandler.class);

  private final AssignDistributionPlanBrigadeCommandHandler assignBrigade;
  private final TransactionTemplate newTransaction;

  public DistributionPlanApprovedEventHandler(
      AssignDistributionPlanBrigadeCommandHandler assignBrigade,
      PlatformTransactionManager transactionManager) {
    this.assignBrigade = assignBrigade;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(DistributionPlanApproved event) {
    var command =
        new AssignDistributionPlanBrigadeCommand(
            event.planId(), event.organizationId(), Optional.empty());
    try {
      newTransaction.executeWithoutResult(status -> assignBrigade.handle(command));
    } catch (NoAvailableBrigadeException | ExternalServiceUnavailableException ex) {
      LOGGER.warn("Plan {} stays without a brigade: {}", event.planId().value(), ex.getMessage());
    } catch (RuntimeException ex) {
      LOGGER.error("Plan {} could not get a brigade", event.planId().value(), ex);
    }
  }
}
