package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A shift change and the zones that passed to the incoming authority (US-23). */
public record ShiftHandoverResource(
    UUID id,
    UUID outgoingAuthorityId,
    UUID incomingAuthorityId,
    LocalDateTime handedOverAt,
    String notes,
    List<UUID> zoneIds) {}
