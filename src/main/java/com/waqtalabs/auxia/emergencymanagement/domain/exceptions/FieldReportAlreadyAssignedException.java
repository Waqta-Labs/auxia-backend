package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** The zone of a field report is assigned only once. */
public class FieldReportAlreadyAssignedException extends ConflictException {

  public FieldReportAlreadyAssignedException(FieldReportId id) {
    super("Field report " + id.value() + " already has a zone", id.value());
  }
}
