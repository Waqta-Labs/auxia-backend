package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDate;

/** A new emergency is active. Citizen Transparency shows it to the public. */
public record EmergencyRegistered(
    EmergencyId emergencyId,
    String name,
    EmergencyType type,
    AdministrativeLocation location,
    LocalDate startDate)
    implements DomainEvent {}
