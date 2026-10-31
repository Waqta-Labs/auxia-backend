package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryReport;
import java.util.Locale;

/** Turns the history of deliveries into a document for an audit, such as a PDF (US-19). */
public interface DeliveryReportRenderer {

  byte[] render(DeliveryReport report, Locale locale);
}
