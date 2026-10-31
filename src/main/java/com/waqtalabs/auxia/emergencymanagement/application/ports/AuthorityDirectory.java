package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Asks Identity Access who the authorities are: the organization of the next responsible at a
 * shift change (US-23), and the names shown in the history of a zone (US-20).
 */
public interface AuthorityDirectory {

  /** Empty if the user does not exist. */
  Optional<OrganizationId> organizationOf(AuthorityId authority);

  /** Full name of each authority that exists; an unknown one is left out. */
  Map<AuthorityId, String> fullNames(Collection<AuthorityId> authorities);
}
