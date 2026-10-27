package com.waqtalabs.auxia.identityaccess.domain.repositories;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

  UserAccount save(UserAccount userAccount);

  Optional<UserAccount> findById(UserId id);

  Optional<UserAccount> findByEmail(EmailAddress email);

  boolean existsByEmail(EmailAddress email);

  /** Users of the organization, ordered by email. */
  List<UserAccount> findByOrganizationId(OrganizationId organizationId);
}
