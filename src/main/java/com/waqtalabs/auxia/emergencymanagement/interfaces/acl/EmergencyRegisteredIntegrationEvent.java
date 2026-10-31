package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import java.time.LocalDate;
import java.util.UUID;

/** Public version of {@code EmergencyRegistered}: the data a citizen needs to recognize it. */
public record EmergencyRegisteredIntegrationEvent(
    UUID emergencyId,
    String name,
    String type,
    String department,
    String province,
    String district,
    LocalDate startDate) {}
