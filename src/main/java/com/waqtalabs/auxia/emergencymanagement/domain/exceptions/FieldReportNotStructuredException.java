package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Only the indicators of a report already structured can be corrected (US-03). */
public class FieldReportNotStructuredException extends ConflictException {

  public FieldReportNotStructuredException(FieldReportId id) {
    super("Field report " + id.value() + " is not structured yet", id.value());
  }
}
