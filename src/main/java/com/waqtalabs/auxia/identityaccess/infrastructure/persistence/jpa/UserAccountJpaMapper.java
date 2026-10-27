package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;

final class UserAccountJpaMapper {

  private UserAccountJpaMapper() {}

  static UserAccount toDomain(UserAccountJpaEntity entity) {
    return UserAccount.reconstitute(
        new UserId(entity.id),
        new OrganizationId(entity.organizationId),
        new EmailAddress(entity.email),
        new PersonName(entity.firstName, entity.lastName),
        new PasswordHash(entity.passwordHash, entity.hashAlgorithm),
        entity.role,
        entity.status,
        entity.failedLoginAttempts,
        entity.lastLoginAt);
  }

  static void copy(UserAccount account, UserAccountJpaEntity entity) {
    entity.id = account.id().value();
    entity.organizationId = account.organizationId().value();
    entity.email = account.email().value();
    entity.passwordHash = account.passwordHash().value();
    entity.hashAlgorithm = account.passwordHash().algorithm();
    entity.firstName = account.name().firstName();
    entity.lastName = account.name().lastName();
    entity.role = account.role();
    entity.status = account.status();
    entity.failedLoginAttempts = account.failedLoginAttempts();
    entity.lastLoginAt = account.lastLoginAt().orElse(null);
  }
}
