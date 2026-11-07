package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import java.util.Objects;
import java.util.Optional;

/**
 * How the SOS of a citizen is being attended, as "Mis reportes" shows it (A_05): prioritized by
 * the AI with the Urgency Score of its zone, and the brigade on the way with its ETA. It tells
 * nothing about other reports or people.
 *
 * @param score urgency score of the zone, from 0 to 100, once the SOS is prioritized
 * @param etaMinutes minutes until the brigade arrives, while it is on the way and both its base
 *     and the zone have coordinates
 */
public record CitizenReportProgress(
    Stage stage, Optional<Double> score, Optional<Integer> etaMinutes) {

  public CitizenReportProgress {
    Objects.requireNonNull(stage, "stage");
    Objects.requireNonNull(score, "score");
    Objects.requireNonNull(etaMinutes, "etaMinutes");
  }

  /** The stages that the citizen sees once the SOS reached the API. */
  public enum Stage {
    /** Stored, but not prioritized yet: without a zone, or its zone has no score. */
    RECEIVED,
    /** In a zone that has an urgency score or a manual priority ("Priorizado por IA"). */
    PRIORITIZED,
    /**
     * In a zone with an approved plan that a brigade is delivering ("Brigada en camino"). After
     * the delivery the SOS is {@code PRIORITIZED} again.
     */
    BRIGADE_ON_THE_WAY
  }
}
