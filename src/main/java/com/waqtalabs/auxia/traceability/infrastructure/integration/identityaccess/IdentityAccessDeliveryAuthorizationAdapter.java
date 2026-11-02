package com.waqtalabs.auxia.traceability.infrastructure.integration.identityaccess;

import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import com.waqtalabs.auxia.traceability.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Component;

/** Adapter to the Open Host Service of Identity Access. */
@Component
class IdentityAccessDeliveryAuthorizationAdapter implements AuthorizationPort {

  private static final String FIELD_BRIGADE = "FIELD_BRIGADE";
  private static final String AUTHORITY = "AUTHORITY";

  private final IdentityAccessContextFacade identityAccess;

  IdentityAccessDeliveryAuthorizationAdapter(IdentityAccessContextFacade identityAccess) {
    this.identityAccess = identityAccess;
  }

  @Override
  public boolean canRegisterDeliveries(UserId userId) {
    return identityAccess.isActiveUserWithRole(userId.value(), FIELD_BRIGADE, AUTHORITY);
  }
}
