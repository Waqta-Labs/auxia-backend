package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/** A recommendation produced a plan for a zone. */
public record DistributionPlanProposed(
    DistributionPlanId planId, EmergencyId emergencyId, ZoneId zoneId)
    implements DomainEvent {}
