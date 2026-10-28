package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/** An authority changed the quantities of a proposed plan. */
public record DistributionPlanModified(
    DistributionPlanId planId, AuthorityId modifiedBy)
    implements DomainEvent {}
