package com.waqtalabs.auxia.identityaccess.application.queryservices;

import com.waqtalabs.auxia.identityaccess.application.queries.GetUserAccountByIdQuery;
import com.waqtalabs.auxia.identityaccess.application.queries.GetUserAccountsByOrganizationIdQuery;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.UserAccountNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserAccountQueryService {

  private final UserAccountRepository userAccountRepository;

  public UserAccountQueryService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  public UserAccount handle(GetUserAccountByIdQuery query) {
    return userAccountRepository
        .findById(query.userId())
        .orElseThrow(() -> new UserAccountNotFoundException(query.userId()));
  }

  /** Users of the organization, ordered by email. */
  public List<UserAccount> handle(GetUserAccountsByOrganizationIdQuery query) {
    return userAccountRepository.findByOrganizationId(query.organizationId());
  }
}
