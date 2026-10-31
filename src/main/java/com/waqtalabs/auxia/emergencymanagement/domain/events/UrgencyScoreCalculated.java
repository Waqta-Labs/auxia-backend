package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/**
 * A zone has a new urgency score and is prioritized. The event does not carry the score:
 * Citizen Transparency only learns that the zone moved on.
 */
public record UrgencyScoreCalculated(ZoneId zoneId) implements DomainEvent {}
