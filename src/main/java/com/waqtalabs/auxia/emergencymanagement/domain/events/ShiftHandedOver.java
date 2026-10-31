package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ShiftHandoverId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;

/** The zones of an authority passed to the next one at a shift change (US-23). */
public record ShiftHandedOver(
    ShiftHandoverId handoverId,
    AuthorityId outgoing,
    AuthorityId incoming,
    List<ZoneId> zoneIds,
    String notes,
    LocalDateTime handedOverAt)
    implements DomainEvent {}
