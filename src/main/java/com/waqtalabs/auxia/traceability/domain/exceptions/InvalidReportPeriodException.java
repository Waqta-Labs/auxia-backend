package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import java.time.LocalDate;

/** The period of a report starts after it ends. */
public class InvalidReportPeriodException extends RuleViolationException {

  public InvalidReportPeriodException(LocalDate from, LocalDate to) {
    super("The period from " + from + " to " + to + " ends before it starts", from, to);
  }
}
