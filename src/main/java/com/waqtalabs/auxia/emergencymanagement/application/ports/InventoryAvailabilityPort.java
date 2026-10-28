package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import java.util.List;

/** Stock that an organization can still distribute, from Resource Management. */
public interface InventoryAvailabilityPort {

  /** Every resource of the inventory of the organization; empty when it has no inventory. */
  List<AvailableResource> availableStock(OrganizationId organizationId);
}
