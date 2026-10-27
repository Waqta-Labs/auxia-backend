package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneReport;
import java.util.Locale;

/** Turns the report of a zone into a document that can be shared, such as a PDF. */
public interface ZoneReportRenderer {

  byte[] render(ZoneReport report, Locale locale);
}
