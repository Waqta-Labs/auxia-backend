package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDateTime;

/** An authority took the manual priority off a zone; the score orders it again. */
public record ZoneManualPriorityRemoved(
    ZoneId zoneId, AuthorityId removedBy, LocalDateTime removedAt) implements DomainEvent {}
