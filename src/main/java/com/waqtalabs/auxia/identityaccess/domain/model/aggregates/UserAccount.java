package com.waqtalabs.auxia.identityaccess.domain.model.aggregates;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Account of a user, with its role and status. A user belongs to one organization, set when the
 * account is registered and never changed.
 *
 * <p>After {@value #MAX_FAILED_ATTEMPTS} failed sign-ins in a row the account is locked until an
 * administrator unlocks it. {@link #authenticate} only answers true or false, so the application
 * can answer every failure with the same generic message (US-21).
 */
public final class UserAccount {

  public static final int MAX_FAILED_ATTEMPTS = 5;

  private final UserId id;
  private final OrganizationId organizationId;
  private final EmailAddress email;
  private final PersonName name;
  private PasswordHash passwordHash;
  private Role role;
  private AccountStatus status;
  private int failedLoginAttempts;
  private LocalDateTime lastLoginAt;

  private UserAccount(
      UserId id,
      OrganizationId organizationId,
      EmailAddress email,
      PersonName name,
      PasswordHash passwordHash,
      Role role,
      AccountStatus status,
      int failedLoginAttempts,
      LocalDateTime lastLoginAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.email = Objects.requireNonNull(email, "email");
    this.name = Objects.requireNonNull(name, "name");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.role = Objects.requireNonNull(role, "role");
    this.status = Objects.requireNonNull(status, "status");
    if (failedLoginAttempts < 0 || failedLoginAttempts > MAX_FAILED_ATTEMPTS) {
      throw new IllegalArgumentException("failedLoginAttempts out of range");
    }
    this.failedLoginAttempts = failedLoginAttempts;
    this.lastLoginAt = lastLoginAt;
  }

  /** Registers an active account. */
  public static UserAccount register(
      OrganizationId organizationId,
      EmailAddress email,
      PersonName name,
      Role role,
      PasswordHash passwordHash) {
    return new UserAccount(
        UserId.generate(),
        organizationId,
        email,
        name,
        passwordHash,
        role,
        AccountStatus.ACTIVE,
        0,
        null);
  }

  /**
   * Rebuilds an account that is already stored.
   *
   * @param lastLoginAt {@code null} if the user never signed in
   */
  public static UserAccount reconstitute(
      UserId id,
      OrganizationId organizationId,
      EmailAddress email,
      PersonName name,
      PasswordHash passwordHash,
      Role role,
      AccountStatus status,
      int failedLoginAttempts,
      LocalDateTime lastLoginAt) {
    return new UserAccount(
        id,
        organizationId,
        email,
        name,
        passwordHash,
        role,
        status,
        failedLoginAttempts,
        lastLoginAt);
  }

  /**
   * Checks a password. A match on an active account resets the failed attempts and records the
   * sign-in; a wrong password adds an attempt and the fifth one locks the account. A locked or
   * disabled account never authenticates.
   *
   * @return whether the user is authenticated
   */
  public boolean authenticate(
      String rawPassword, PasswordHashingService hashing, LocalDateTime now) {
    if (!isActive()) {
      return false;
    }
    if (hashing.matches(rawPassword, passwordHash)) {
      failedLoginAttempts = 0;
      lastLoginAt = Objects.requireNonNull(now, "now");
      return true;
    }
    failedLoginAttempts++;
    if (failedLoginAttempts == MAX_FAILED_ATTEMPTS) {
      status = AccountStatus.LOCKED;
    }
    return false;
  }

  public void changeRole(Role role) {
    this.role = Objects.requireNonNull(role, "role");
  }

  public void changePassword(PasswordHash passwordHash) {
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
  }

  public void disable() {
    status = AccountStatus.DISABLED;
  }

  /** Makes a locked or disabled account active again, with no failed attempts. */
  public void unlock() {
    status = AccountStatus.ACTIVE;
    failedLoginAttempts = 0;
  }

  public boolean hasRole(Role role) {
    return this.role == role;
  }

  public boolean isActive() {
    return status == AccountStatus.ACTIVE;
  }

  public boolean belongsTo(OrganizationId organizationId) {
    return this.organizationId.equals(organizationId);
  }

  public UserId id() {
    return id;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public EmailAddress email() {
    return email;
  }

  public PersonName name() {
    return name;
  }

  public PasswordHash passwordHash() {
    return passwordHash;
  }

  public Role role() {
    return role;
  }

  public AccountStatus status() {
    return status;
  }

  public int failedLoginAttempts() {
    return failedLoginAttempts;
  }

  /** Empty if the user never signed in. */
  public Optional<LocalDateTime> lastLoginAt() {
    return Optional.ofNullable(lastLoginAt);
  }
}
