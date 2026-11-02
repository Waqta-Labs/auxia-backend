package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Two brigades of the same organization cannot have the same name. */
public class DuplicateBrigadeException extends ConflictException {

  public DuplicateBrigadeException(String name) {
    super("The organization already has a brigade named " + name, name);
  }
}
