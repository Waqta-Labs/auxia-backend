package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExport;
import java.time.LocalDateTime;

/**
 * Sends the field reports of an emergency to SINPAD, so the authority does not register them
 * twice (US-33). Sending the same emergency again replaces its reports in SINPAD.
 */
public interface SinpadGateway {

  /**
   * @throws com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException
   *     if SINPAD does not answer or rejects the reports
   */
  SinpadRegistration submit(SinpadExport export);

  /**
   * What SINPAD answered.
   *
   * @param registrationCode number of the emergency in SINPAD, the same in every submission
   */
  record SinpadRegistration(
      String registrationCode, int reportsReceived, LocalDateTime receivedAt) {}
}
