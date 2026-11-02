package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/**
 * The outgoing authority, who signed in, hands over its zones to the incoming one. {@code notes}
 * is optional.
 */
public record HandOverShiftCommand(
    OrganizationId organizationId,
    AuthorityId outgoingAuthorityId,
    AuthorityId incomingAuthorityId,
    String notes) {}
