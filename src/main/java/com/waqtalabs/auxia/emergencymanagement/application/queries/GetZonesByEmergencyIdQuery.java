package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;

public record GetZonesByEmergencyIdQuery(EmergencyId emergencyId) {}
