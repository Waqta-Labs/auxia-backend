package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;

public record CloseEmergencyCommand(EmergencyId emergencyId) {}
