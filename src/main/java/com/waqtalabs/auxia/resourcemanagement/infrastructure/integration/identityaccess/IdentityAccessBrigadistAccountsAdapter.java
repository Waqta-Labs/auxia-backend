package com.waqtalabs.auxia.resourcemanagement.infrastructure.integration.identityaccess;

import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import com.waqtalabs.auxia.resourcemanagement.application.ports.BrigadistAccounts;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Component;

/** Adapter to the accounts of Identity Access, through its Open Host Service. */
@Component
class IdentityAccessBrigadistAccountsAdapter implements BrigadistAccounts {

  private final IdentityAccessContextFacade identityAccess;

  IdentityAccessBrigadistAccountsAdapter(IdentityAccessContextFacade identityAccess) {
    this.identityAccess = identityAccess;
  }

  @Override
  public boolean isActiveBrigadist(UserId userId, OrganizationId organizationId) {
    return identityAccess.isActiveUserWithRole(userId.value(), "FIELD_BRIGADE")
        && identityAccess
            .getOrganizationId(userId.value())
            .filter(organizationId.value()::equals)
            .isPresent();
  }
}
