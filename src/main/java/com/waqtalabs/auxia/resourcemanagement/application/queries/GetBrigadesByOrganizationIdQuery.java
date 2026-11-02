package com.waqtalabs.auxia.resourcemanagement.application.queries;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;

/**
 * Brigades of an organization.
 *
 * @param status only the brigades in this status, or every brigade when {@code null}
 */
public record GetBrigadesByOrganizationIdQuery(
    OrganizationId organizationId, BrigadeStatus status) {}
