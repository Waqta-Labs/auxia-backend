package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

public record ZoneRegistered(ZoneId zoneId) implements DomainEvent {}
