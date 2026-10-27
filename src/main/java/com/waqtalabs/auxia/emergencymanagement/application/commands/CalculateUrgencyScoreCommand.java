package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

public record CalculateUrgencyScoreCommand(ZoneId zoneId) {}
