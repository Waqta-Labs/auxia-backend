package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** An operation would take more than the available quantity of a resource (TS-03). */
public class InsufficientStockException extends ConflictException {

  public InsufficientStockException(ResourceId resourceId, int available, int requested) {
    super(
        "Resource "
            + resourceId.value()
            + " has "
            + available
            + " available and "
            + requested
            + " were requested",
        resourceId.value(),
        available,
        requested);
  }
}
