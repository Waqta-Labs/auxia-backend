package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;

/** Extracts the indicators of a field report with NLP (US-02). The AI Service implements it. */
public interface FieldReportStructuringService {

  /**
   * Structures the text of a field report, written in Spanish.
   *
   * @throws ExternalServiceUnavailableException if the NLP service does not answer
   */
  StructuredFieldData structure(String rawText);
}
