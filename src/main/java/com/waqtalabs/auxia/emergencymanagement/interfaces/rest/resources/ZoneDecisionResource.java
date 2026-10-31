package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A decision of the history of a zone (US-20). {@code detail} is the reason or justification, if
 * the decision has one. {@code subjectId} is the plan, the field report or the shift handover the
 * decision is about, and {@code handedOverTo} the next responsible, only in a shift handover. A
 * name is {@code null} if the user no longer exists.
 */
public record ZoneDecisionResource(
    ZoneDecisionType type,
    LocalDateTime decidedAt,
    UUID decidedBy,
    String decidedByName,
    String detail,
    UUID subjectId,
    UUID handedOverTo,
    String handedOverToName) {}
