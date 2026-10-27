package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

public record FieldReportRegistered(FieldReportId fieldReportId) implements DomainEvent {}
