package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import java.time.LocalDate;
import java.util.UUID;

public record EmergencyResource(
    UUID id,
    String name,
    EmergencyType type,
    String description,
    AdministrativeLocationResource location,
    EmergencyStatus status,
    LocalDate startDate) {}
