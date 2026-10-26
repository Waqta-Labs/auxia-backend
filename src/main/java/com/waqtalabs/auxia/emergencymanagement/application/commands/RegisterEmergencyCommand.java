package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import java.time.LocalDate;

/** Registers a new emergency. {@code description} is optional. */
public record RegisterEmergencyCommand(
    String name,
    EmergencyType type,
    String description,
    AdministrativeLocation location,
    LocalDate startDate) {}
