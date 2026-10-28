package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** Modifying or rejecting a plan needs a reason, for a later audit (US-14). */
public class JustificationRequiredException extends RuleViolationException {

  public JustificationRequiredException(DistributionPlanId id) {
    super(
        "Distribution plan " + id.value() + " needs a justification for this decision",
        id.value());
  }
}
