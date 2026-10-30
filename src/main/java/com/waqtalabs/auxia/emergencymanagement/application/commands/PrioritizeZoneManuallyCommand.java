package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

/** Marks a zone as a priority by hand, with its justification (US-07). */
public record PrioritizeZoneManuallyCommand(
    ZoneId zoneId, AuthorityId authorityId, String reason) {}
