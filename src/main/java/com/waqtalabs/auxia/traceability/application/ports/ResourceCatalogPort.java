package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import java.util.Map;

/** Asks Resource Management the names of the resources of an organization (US-19). */
public interface ResourceCatalogPort {

  /** Name of each resource of the inventory of the organization. */
  Map<ResourceId, String> resourceNames(OrganizationId organizationId);
}
