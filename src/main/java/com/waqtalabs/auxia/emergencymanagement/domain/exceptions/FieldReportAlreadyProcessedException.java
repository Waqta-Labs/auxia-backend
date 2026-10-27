package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A field report is structured only once (US-02). */
public class FieldReportAlreadyProcessedException extends ConflictException {

  public FieldReportAlreadyProcessedException(FieldReportId id) {
    super("Field report " + id.value() + " is already structured", id.value());
  }
}
