package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.identityaccess;

import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import org.springframework.stereotype.Component;

/** Adapter to the Open Host Service of Identity Access. */
@Component
class IdentityAccessAuthorizationAdapter implements AuthorizationPort {

  private static final String AUTHORITY = "AUTHORITY";

  private final IdentityAccessContextFacade identityAccess;

  IdentityAccessAuthorizationAdapter(IdentityAccessContextFacade identityAccess) {
    this.identityAccess = identityAccess;
  }

  @Override
  public boolean isActiveAuthority(AuthorityId authorityId) {
    return identityAccess.isActiveUserWithRole(authorityId.value(), AUTHORITY);
  }
}
