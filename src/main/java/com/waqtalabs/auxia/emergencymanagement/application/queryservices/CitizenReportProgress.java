package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import java.util.Objects;
import java.util.Optional;

/**
 * How the SOS of a citizen is being attended. It tells nothing about other reports or people,
 * nor the urgency score of its zone: like the public view, it does not reveal the internal
 * variables of the prioritization (TS-C02, R-04).
 *
 * @param etaMinutes minutes until the brigade arrives, when they can be estimated
 */
public record CitizenReportProgress(Stage stage, Optional<Integer> etaMinutes) {

  public CitizenReportProgress {
    Objects.requireNonNull(stage, "stage");
    Objects.requireNonNull(etaMinutes, "etaMinutes");
  }

  /** The stages that the citizen sees. */
  public enum Stage {
    /** Stored, pending review or without a zone yet. */
    RECEIVED,
    /** In a zone that has an urgency score or a manual priority. */
    PRIORITIZED,
    /** In a zone with an approved plan that a brigade took. */
    BRIGADE_ON_THE_WAY
  }
}
