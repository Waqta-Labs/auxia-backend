package com.waqtalabs.auxia.identityaccess.application.commands;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;

/** An administrator registers a user of their own organization. */
public record RegisterUserCommand(
    OrganizationId organizationId,
    String email,
    String firstName,
    String lastName,
    Role role,
    String password) {}
