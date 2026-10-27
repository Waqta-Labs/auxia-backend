package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Two resources of the same inventory cannot have the same name. */
public class DuplicateResourceException extends ConflictException {

  public DuplicateResourceException(String name) {
    super("The inventory already has a resource named " + name, name);
  }
}
