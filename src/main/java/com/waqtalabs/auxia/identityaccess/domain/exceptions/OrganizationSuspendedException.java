package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A suspended organization cannot get new users. */
public class OrganizationSuspendedException extends ConflictException {

  public OrganizationSuspendedException(OrganizationId id) {
    super("Organization " + id.value() + " is suspended", id.value());
  }
}
