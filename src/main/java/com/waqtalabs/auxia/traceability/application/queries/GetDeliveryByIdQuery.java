package com.waqtalabs.auxia.traceability.application.queries;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;

/** A delivery of the organization of the user. */
public record GetDeliveryByIdQuery(DeliveryId deliveryId, OrganizationId organizationId) {}
