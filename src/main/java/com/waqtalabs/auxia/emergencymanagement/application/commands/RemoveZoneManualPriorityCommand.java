package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

/** Takes the manual priority off a zone. */
public record RemoveZoneManualPriorityCommand(ZoneId zoneId, AuthorityId authorityId) {}
