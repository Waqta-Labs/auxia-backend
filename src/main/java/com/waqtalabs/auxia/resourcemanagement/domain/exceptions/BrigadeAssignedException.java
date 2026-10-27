package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** The availability of a brigade changes only when it has no assignment in progress. */
public class BrigadeAssignedException extends ConflictException {

  public BrigadeAssignedException(BrigadeId id) {
    super("Brigade " + id.value() + " has an assignment in progress", id.value());
  }
}
