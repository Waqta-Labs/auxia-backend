package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;

/** A delivery is registered with at least one evidence (US-15, scenario 2). */
public class EvidenceRequiredException extends RuleViolationException {

  public EvidenceRequiredException(DeliveryId id) {
    super("Delivery " + id.value() + " needs at least one evidence", id.value());
  }
}
