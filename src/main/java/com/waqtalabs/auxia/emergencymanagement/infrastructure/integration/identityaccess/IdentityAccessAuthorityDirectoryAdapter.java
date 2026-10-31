package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.identityaccess;

import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorityDirectory;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adapter to the Open Host Service of Identity Access. */
@Component
class IdentityAccessAuthorityDirectoryAdapter implements AuthorityDirectory {

  private final IdentityAccessContextFacade identityAccess;

  IdentityAccessAuthorityDirectoryAdapter(IdentityAccessContextFacade identityAccess) {
    this.identityAccess = identityAccess;
  }

  @Override
  public Optional<OrganizationId> organizationOf(AuthorityId authority) {
    return identityAccess.getOrganizationId(authority.value()).map(OrganizationId::new);
  }

  @Override
  public Map<AuthorityId, String> fullNames(Collection<AuthorityId> authorities) {
    return identityAccess
        .getFullNames(authorities.stream().map(AuthorityId::value).toList())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(entry -> new AuthorityId(entry.getKey()), Map.Entry::getValue));
  }
}
