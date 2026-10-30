package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;

/** A delivery is registered only once; a hash already computed never changes. */
public class DeliveryAlreadyRegisteredException extends ConflictException {

  public DeliveryAlreadyRegisteredException(DeliveryId id, DeliveryStatus status) {
    super("Delivery " + id.value() + " is already " + status, id.value(), status);
  }
}
