package com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects;

/**
 * Stage of attention of a zone that a citizen sees, in the order of refinement R-04. Within one
 * distribution plan a zone only moves forward.
 */
public enum PublicStage {
  REGISTERED,
  PRIORITIZED,
  AID_APPROVED,
  AID_IN_TRANSIT,
  AID_DELIVERED;

  public boolean isAfter(PublicStage other) {
    return compareTo(other) > 0;
  }
}
