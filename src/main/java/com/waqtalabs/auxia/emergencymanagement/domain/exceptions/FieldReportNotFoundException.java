package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class FieldReportNotFoundException extends NotFoundException {

  public FieldReportNotFoundException(FieldReportId id) {
    super("Field report " + id.value() + " does not exist", id.value());
  }
}
