package com.waqtalabs.auxia.identityaccess.application.queryservices;

import com.waqtalabs.auxia.identityaccess.application.queries.CheckAccessQuery;
import com.waqtalabs.auxia.identityaccess.application.queries.GetActiveUserAccountQuery;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms, on each request and when a critical command runs, that the user exists, that their
 * account and their organization are active and that they have one of the roles asked for
 * (QAD-02).
 *
 * <p>It reads the account and the organization every time instead of caching them, so a role
 * change or a suspension applies at once, even to a token issued before it (R-05).
 */
@Service
@Transactional(readOnly = true)
public class CheckAccessQueryHandler {

  private final UserAccountRepository userAccountRepository;
  private final OrganizationRepository organizationRepository;

  public CheckAccessQueryHandler(
      UserAccountRepository userAccountRepository,
      OrganizationRepository organizationRepository) {
    this.userAccountRepository = userAccountRepository;
    this.organizationRepository = organizationRepository;
  }

  public Optional<UserAccount> handle(GetActiveUserAccountQuery query) {
    return userAccountRepository
        .findById(query.userId())
        .filter(UserAccount::isActive)
        .filter(
            account ->
                organizationRepository
                    .findById(account.organizationId())
                    .map(Organization::isActive)
                    .orElse(false));
  }

  public boolean handle(CheckAccessQuery query) {
    return handle(new GetActiveUserAccountQuery(query.userId()))
        .filter(account -> query.roles().stream().anyMatch(account::hasRole))
        .isPresent();
  }
}
