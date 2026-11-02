package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.SubmitToSinpadCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SinpadGateway;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SinpadGateway.SinpadRegistration;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IncompleteSinpadReportsException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoFieldReportsToExportException;
import org.springframework.stereotype.Service;

/**
 * Sends the field reports of an emergency to SINPAD (US-33): the same rows as the export file,
 * with the same checks, so an incomplete report is never sent.
 */
@Service
public class SubmitToSinpadCommandHandler {

  private final SinpadExportQueryService exports;
  private final SinpadGateway sinpad;

  public SubmitToSinpadCommandHandler(SinpadExportQueryService exports, SinpadGateway sinpad) {
    this.exports = exports;
    this.sinpad = sinpad;
  }

  /**
   * @throws EmergencyNotFoundException if the emergency does not exist
   * @throws NoFieldReportsToExportException if the emergency has no field reports
   * @throws IncompleteSinpadReportsException if a report lacks a field SINPAD requires
   */
  public SinpadRegistration handle(SubmitToSinpadCommand command) {
    return sinpad.submit(exports.export(command.emergencyId()));
  }
}
