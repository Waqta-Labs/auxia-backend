package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** No delivery was registered for the zone or period of the report (US-19, scenario 2). */
public class NoDeliveriesToReportException extends NotFoundException {

  public NoDeliveriesToReportException() {
    super("No delivery was registered for the zone or period requested");
  }
}
