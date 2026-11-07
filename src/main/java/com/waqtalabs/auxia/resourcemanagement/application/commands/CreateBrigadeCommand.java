package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;

/**
 * Creates a brigade.
 *
 * @param code what a brigadist writes to register (A_02), or {@code null} to generate one
 */
public record CreateBrigadeCommand(
    OrganizationId organizationId, String name, Location baseLocation, String code) {

  /** A brigade with a generated code. */
  public CreateBrigadeCommand(OrganizationId organizationId, String name, Location baseLocation) {
    this(organizationId, name, baseLocation, null);
  }
}
