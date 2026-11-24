package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class CitizenReportNotFoundException extends NotFoundException {

  public CitizenReportNotFoundException(CitizenReportId id) {
    super("Citizen report " + id.value() + " does not exist", id.value());
  }
}
