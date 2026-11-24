package com.waqtalabs.auxia.identityaccess.application.commands;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;

/**
 * Approves or rejects the request of a brigadist.
 *
 * @param organizationId organization of the administrator who decides
 * @param rejectionReason {@code null} to approve it
 */
public record DecideBrigadeRegistrationCommand(
    BrigadeRegistrationId registrationId,
    OrganizationId organizationId,
    UserId decidedBy,
    String rejectionReason) {}
