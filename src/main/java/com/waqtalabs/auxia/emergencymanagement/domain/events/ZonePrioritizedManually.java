package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDateTime;

/** An authority marked a zone as a priority by hand (US-07). */
public record ZonePrioritizedManually(
    ZoneId zoneId, AuthorityId setBy, String reason, LocalDateTime setAt)
    implements DomainEvent {}
