package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import java.time.LocalDateTime;
import java.util.UUID;

/** A user account without its password hash. {@code lastLoginAt} is null before the first one. */
public record UserAccountResource(
    UUID id,
    UUID organizationId,
    String email,
    String firstName,
    String lastName,
    Role role,
    AccountStatus status,
    LocalDateTime lastLoginAt) {}
