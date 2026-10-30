package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;

/** Each resource appears once in a delivery. */
public class DuplicateDeliveryResourceException extends RuleViolationException {

  public DuplicateDeliveryResourceException(ResourceId resourceId) {
    super("Resource " + resourceId.value() + " appears more than once", resourceId.value());
  }
}
