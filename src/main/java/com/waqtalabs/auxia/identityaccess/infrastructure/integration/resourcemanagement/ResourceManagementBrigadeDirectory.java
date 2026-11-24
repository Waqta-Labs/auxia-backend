package com.waqtalabs.auxia.identityaccess.infrastructure.integration.resourcemanagement;

import com.waqtalabs.auxia.identityaccess.application.ports.BrigadeDirectory;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ResourceManagementContextFacade;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapter to the brigades of Resource Management, through its Open Host Service. */
@Component
class ResourceManagementBrigadeDirectory implements BrigadeDirectory {

  private final ResourceManagementContextFacade resourceManagement;

  ResourceManagementBrigadeDirectory(ResourceManagementContextFacade resourceManagement) {
    this.resourceManagement = resourceManagement;
  }

  @Override
  public Optional<BrigadeReference> findByCode(String code) {
    return resourceManagement
        .findBrigadeByCode(code)
        .map(
            brigade ->
                new BrigadeReference(
                    brigade.brigadeId(), new OrganizationId(brigade.organizationId())));
  }

  @Override
  public void addBrigadist(
      UUID brigadeId, OrganizationId organizationId, String fullName, String phone, UserId user) {
    resourceManagement.addBrigadist(
        brigadeId, organizationId.value(), fullName, phone, user.value());
  }
}
