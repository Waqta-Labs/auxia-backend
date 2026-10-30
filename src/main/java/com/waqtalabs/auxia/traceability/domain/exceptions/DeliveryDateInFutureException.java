package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import java.time.LocalDateTime;

/** A delivery cannot happen after it is registered. */
public class DeliveryDateInFutureException extends RuleViolationException {

  public DeliveryDateInFutureException(LocalDateTime deliveredAt) {
    super("Delivery date " + deliveredAt + " is in the future", deliveredAt);
  }
}
