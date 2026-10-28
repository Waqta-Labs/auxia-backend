package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/** An authority rejected a plan; the stock it held is available again. */
public record DistributionPlanRejected(
    DistributionPlanId planId, ZoneId zoneId, AuthorityId rejectedBy)
    implements DomainEvent {}
