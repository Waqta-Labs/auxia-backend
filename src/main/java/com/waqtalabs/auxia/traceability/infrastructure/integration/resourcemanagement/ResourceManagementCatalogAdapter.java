package com.waqtalabs.auxia.traceability.infrastructure.integration.resourcemanagement;

import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.AvailableStockResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ResourceManagementContextFacade;
import com.waqtalabs.auxia.traceability.application.ports.ResourceCatalogPort;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adapter to the inventory of Resource Management, through its Open Host Service. */
@Component
class ResourceManagementCatalogAdapter implements ResourceCatalogPort {

  private final ResourceManagementContextFacade resourceManagement;

  ResourceManagementCatalogAdapter(ResourceManagementContextFacade resourceManagement) {
    this.resourceManagement = resourceManagement;
  }

  @Override
  public Map<ResourceId, String> resourceNames(OrganizationId organizationId) {
    return resourceManagement.fetchAvailableStock(organizationId.value()).stream()
        .collect(
            Collectors.toMap(
                resource -> new ResourceId(resource.resourceId()), AvailableStockResource::name));
  }
}
