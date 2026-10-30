package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;

/** A delivery gives at most what its approved plan holds of each resource; it can give less. */
public class DeliveredBeyondPlanException extends RuleViolationException {

  public DeliveredBeyondPlanException(ResourceId resourceId, int delivered, int planned) {
    super(
        "Resource " + resourceId.value() + ": " + delivered + " delivered, " + planned + " planned",
        resourceId.value(),
        delivered,
        planned);
  }
}
