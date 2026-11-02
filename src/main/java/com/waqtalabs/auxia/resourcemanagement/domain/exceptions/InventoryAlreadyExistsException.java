package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Each organization has a single inventory in the MVP (TS-C08). */
public class InventoryAlreadyExistsException extends ConflictException {

  public InventoryAlreadyExistsException(OrganizationId organizationId) {
    super(
        "Organization " + organizationId.value() + " already has an inventory",
        organizationId.value());
  }
}
