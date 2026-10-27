package com.waqtalabs.auxia.identityaccess.application.queries;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;

public record GetUserAccountsByOrganizationIdQuery(OrganizationId organizationId) {}
