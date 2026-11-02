package com.waqtalabs.auxia.identityaccess.application.commands;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;

/**
 * An administrator enables, unlocks or disables an account of their organization.
 *
 * @param status ACTIVE to enable or unlock the account, DISABLED to disable it
 */
public record ChangeAccountStatusCommand(
    UserId administrator, OrganizationId organizationId, UserId userId, AccountStatus status) {}
