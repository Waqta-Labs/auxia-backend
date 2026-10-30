package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;

/** The delivery does not exist, or it belongs to another organization. */
public class DeliveryNotFoundException extends NotFoundException {

  public DeliveryNotFoundException(DeliveryId id) {
    super("Delivery " + id.value() + " does not exist", id.value());
  }
}
