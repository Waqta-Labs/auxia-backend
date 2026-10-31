package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;

/**
 * A decision of the history of a zone with the names of the authorities involved. A name is {@code
 * null} if the user no longer exists in Identity Access.
 *
 * @param handedOverToName only in a shift handover
 */
public record ZoneHistoryEntry(
    ZoneDecision decision, String decidedByName, String handedOverToName) {}
