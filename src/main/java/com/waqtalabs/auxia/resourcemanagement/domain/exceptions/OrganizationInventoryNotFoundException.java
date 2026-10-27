package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The organization has not created its inventory yet. */
public class OrganizationInventoryNotFoundException extends NotFoundException {

  public OrganizationInventoryNotFoundException(OrganizationId organizationId) {
    super("Organization " + organizationId.value() + " has no inventory", organizationId.value());
  }
}
