package com.waqtalabs.auxia.identityaccess.application.commands;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;

/**
 * An administrator changes the role of a user of their organization.
 *
 * @param administrator the administrator who changes the role
 */
public record ChangeUserRoleCommand(
    UserId administrator, OrganizationId organizationId, UserId userId, Role role) {}
