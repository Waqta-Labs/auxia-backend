package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A correction of a report says why, so it can be reviewed later. */
public class CorrectionReasonRequiredException extends RuleViolationException {

  public CorrectionReasonRequiredException(FieldReportId id) {
    super("The correction of field report " + id.value() + " needs a reason", id.value());
  }
}
