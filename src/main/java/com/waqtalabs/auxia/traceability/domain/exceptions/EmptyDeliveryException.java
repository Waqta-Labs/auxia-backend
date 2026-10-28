package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;

/** A registered delivery says which resources were delivered. */
public class EmptyDeliveryException extends RuleViolationException {

  public EmptyDeliveryException(DeliveryId id) {
    super("Delivery " + id.value() + " needs at least one delivered resource", id.value());
  }
}
