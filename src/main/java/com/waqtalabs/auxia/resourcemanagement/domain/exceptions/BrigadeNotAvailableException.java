package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Only an available brigade can be assigned to a distribution plan. */
public class BrigadeNotAvailableException extends ConflictException {

  public BrigadeNotAvailableException(BrigadeId id, BrigadeStatus status) {
    super(
        "Brigade " + id.value() + " is " + status + " and cannot be assigned",
        id.value(),
        status);
  }
}
