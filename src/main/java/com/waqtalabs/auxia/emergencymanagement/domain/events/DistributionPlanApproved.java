package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/** An authority approved a plan, which can now be executed. */
public record DistributionPlanApproved(
    DistributionPlanId planId,
    EmergencyId emergencyId,
    ZoneId zoneId,
    OrganizationId organizationId,
    AuthorityId approvedBy)
    implements DomainEvent {}
