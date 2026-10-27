package com.waqtalabs.auxia.identityaccess.application.commands;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;

public record ChangeOrganizationStatusCommand(
    OrganizationId organizationId, OrganizationStatus status) {}
