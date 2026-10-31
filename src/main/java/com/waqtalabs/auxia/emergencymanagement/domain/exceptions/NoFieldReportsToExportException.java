package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The emergency has no field reports to export to SINPAD (US-33). */
public class NoFieldReportsToExportException extends NotFoundException {

  public NoFieldReportsToExportException(EmergencyId id) {
    super("Emergency " + id.value() + " has no field reports to export", id.value());
  }
}
