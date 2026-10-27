package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;

/**
 * Puts a brigade on or off duty.
 *
 * @param status {@code AVAILABLE} or {@code OFF_DUTY}
 */
public record ChangeBrigadeAvailabilityCommand(BrigadeId brigadeId, BrigadeStatus status) {}
