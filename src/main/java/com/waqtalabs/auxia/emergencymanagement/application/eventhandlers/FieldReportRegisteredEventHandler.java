package com.waqtalabs.auxia.emergencymanagement.application.eventhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.StructureFieldReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.StructureFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportRegistered;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Structures every new field report with NLP once it is stored (US-02).
 *
 * <p>It runs after the registration is committed, in a transaction of its own, so a failure of
 * the AI Service never undoes the registration: the report stays not processed and can be
 * structured again with {@code POST /api/v1/field-reports/{id}/structure}.
 */
@Component
public class FieldReportRegisteredEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FieldReportRegisteredEventHandler.class);

  private final StructureFieldReportCommandHandler structureFieldReport;
  private final TransactionTemplate newTransaction;

  public FieldReportRegisteredEventHandler(
      StructureFieldReportCommandHandler structureFieldReport,
      PlatformTransactionManager transactionManager) {
    this.structureFieldReport = structureFieldReport;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(FieldReportRegistered event) {
    var command = new StructureFieldReportCommand(event.fieldReportId());
    try {
      newTransaction.executeWithoutResult(status -> structureFieldReport.handle(command));
    } catch (ExternalServiceUnavailableException ex) {
      LOGGER.warn(
          "Field report {} stays not processed: {}",
          event.fieldReportId().value(),
          ex.getMessage());
    } catch (RuntimeException ex) {
      LOGGER.error("Field report {} could not be structured", event.fieldReportId().value(), ex);
    }
  }
}
