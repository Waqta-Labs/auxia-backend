package com.waqtalabs.auxia.identityaccess.interfaces.acl;

import com.waqtalabs.auxia.identityaccess.application.queries.CheckAccessQuery;
import com.waqtalabs.auxia.identityaccess.application.queryservices.CheckAccessQueryHandler;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Open Host Service of Identity Access for the other bounded contexts, in the same process. It
 * receives and returns UUIDs and role names only, so no other context depends on its domain.
 *
 * <p>Emergency Management checks the AUTHORITY role before approving or rejecting a plan, and
 * Traceability checks FIELD_BRIGADE or AUTHORITY before registering a delivery. The notifications
 * of every context ask here for the e-mails of their recipients.
 */
@Service
public class IdentityAccessContextFacade {

  private final CheckAccessQueryHandler checkAccess;
  private final UserAccountRepository userAccountRepository;

  public IdentityAccessContextFacade(
      CheckAccessQueryHandler checkAccess, UserAccountRepository userAccountRepository) {
    this.checkAccess = checkAccess;
    this.userAccountRepository = userAccountRepository;
  }

  /**
   * Whether the user is active, in an active organization, and has any of the roles now.
   *
   * @param roles names of roles, such as {@code AUTHORITY}; an unknown name never matches
   */
  public boolean isActiveUserWithRole(UUID userId, String... roles) {
    Set<Role> known =
        Arrays.stream(roles)
            .filter(name -> Arrays.stream(Role.values()).anyMatch(r -> r.name().equals(name)))
            .map(Role::valueOf)
            .collect(Collectors.toSet());
    return !known.isEmpty() && checkAccess.handle(new CheckAccessQuery(new UserId(userId), known));
  }

  /** Organization of the user, or empty if the user does not exist. */
  @Transactional(readOnly = true)
  public Optional<UUID> getOrganizationId(UUID userId) {
    return userAccountRepository
        .findById(new UserId(userId))
        .map(UserAccount::organizationId)
        .map(OrganizationId::value);
  }

  /** Full name of each user that exists; an unknown user is left out. */
  @Transactional(readOnly = true)
  public Map<UUID, String> getFullNames(Collection<UUID> userIds) {
    Map<UUID, String> names = new HashMap<>();
    for (UUID userId : new HashSet<>(userIds)) {
      userAccountRepository
          .findById(new UserId(userId))
          .ifPresent(user -> names.put(userId, user.name().fullName()));
    }
    return names;
  }

  /**
   * E-mails of the active users of the organization with any of the roles, to notify them.
   *
   * @param roles names of roles, such as {@code ORGANIZATION_ADMIN}; an unknown name never matches
   */
  @Transactional(readOnly = true)
  public List<String> getActiveUserEmails(UUID organizationId, String... roles) {
    Set<String> wanted = Set.of(roles);
    return userAccountRepository.findByOrganizationId(new OrganizationId(organizationId)).stream()
        .filter(UserAccount::isActive)
        .filter(user -> wanted.contains(user.role().name()))
        .map(user -> user.email().value())
        .toList();
  }

  /** E-mail of the user, or empty if the user does not exist or is not active. */
  @Transactional(readOnly = true)
  public Optional<String> getActiveUserEmail(UUID userId) {
    return userAccountRepository
        .findById(new UserId(userId))
        .filter(UserAccount::isActive)
        .map(user -> user.email().value());
  }
}
