package com.waqtalabs.auxia.resourcemanagement.application.queries;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;

/** A brigade of the organization of the user; one of another organization is not found. */
public record GetBrigadeByIdQuery(BrigadeId brigadeId, OrganizationId organizationId) {}
