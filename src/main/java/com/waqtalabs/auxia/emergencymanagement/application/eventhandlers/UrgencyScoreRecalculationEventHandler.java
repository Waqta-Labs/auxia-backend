package com.waqtalabs.auxia.emergencymanagement.application.eventhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.CalculateUrgencyScoreCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.CalculateUrgencyScoreCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportAssignedToZone;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportProcessed;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZonePopulationImpactUpdated;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InsufficientScoringDataException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Keeps the urgency score of each zone up to date, so the ranking changes without the authority
 * asking for it: when the zone is registered or its population impact changes, when one of its
 * reports is structured and when a report is assigned to it.
 *
 * <p>Like the structuring, it runs after the commit in a transaction of its own. A zone without
 * population keeps its score not available, and a failure of the AI Service keeps the previous
 * score until {@code POST /api/v1/zones/{id}/score} is called.
 */
@Component
public class UrgencyScoreRecalculationEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UrgencyScoreRecalculationEventHandler.class);

  private final CalculateUrgencyScoreCommandHandler calculateUrgencyScore;
  private final TransactionTemplate newTransaction;

  public UrgencyScoreRecalculationEventHandler(
      CalculateUrgencyScoreCommandHandler calculateUrgencyScore,
      PlatformTransactionManager transactionManager) {
    this.calculateUrgencyScore = calculateUrgencyScore;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(ZoneRegistered event) {
    recalculate(event.zoneId());
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(ZonePopulationImpactUpdated event) {
    recalculate(event.zoneId());
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(FieldReportProcessed event) {
    if (event.zoneId() != null) {
      recalculate(event.zoneId());
    }
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(FieldReportAssignedToZone event) {
    recalculate(event.zoneId());
  }

  private void recalculate(ZoneId zoneId) {
    var command = new CalculateUrgencyScoreCommand(zoneId);
    try {
      newTransaction.executeWithoutResult(status -> calculateUrgencyScore.handle(command));
    } catch (InsufficientScoringDataException ex) {
      LOGGER.info("Zone {} has no population: its urgency score is not available", zoneId.value());
    } catch (ExternalServiceUnavailableException ex) {
      LOGGER.warn("Urgency score of zone {} not updated: {}", zoneId.value(), ex.getMessage());
    } catch (RuntimeException ex) {
      LOGGER.error("Urgency score of zone {} could not be calculated", zoneId.value(), ex);
    }
  }
}
