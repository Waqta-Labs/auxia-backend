package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/**
 * A zone of an emergency was registered. Its score, its public status and its history start from
 * here.
 */
public record ZoneRegistered(
    ZoneId zoneId, EmergencyId emergencyId, String name, AuthorityId registeredBy)
    implements DomainEvent {}
