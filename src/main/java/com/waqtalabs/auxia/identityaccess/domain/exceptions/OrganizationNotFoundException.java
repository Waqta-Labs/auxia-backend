package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class OrganizationNotFoundException extends NotFoundException {

  public OrganizationNotFoundException(OrganizationId id) {
    super("Organization " + id.value() + " does not exist", id.value());
  }
}
