package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The field report was registered by an authority or a brigade, not by the SOS of a citizen. */
public class FieldReportWithoutSosException extends NotFoundException {

  public FieldReportWithoutSosException(FieldReportId id) {
    super("Field report " + id.value() + " does not come from an SOS", id.value());
  }
}
