package com.waqtalabs.auxia.traceability.application.queries;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;

/** Deliveries that the organization of the user made in a zone. */
public record GetDeliveriesByZoneIdQuery(ZoneId zoneId, OrganizationId organizationId) {}
