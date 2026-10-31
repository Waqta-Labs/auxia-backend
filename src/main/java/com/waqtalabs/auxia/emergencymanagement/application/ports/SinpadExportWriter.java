package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExport;

/** Writes the field reports of an emergency in the file layout SINPAD takes (US-33). */
public interface SinpadExportWriter {

  byte[] write(SinpadExport export);
}
