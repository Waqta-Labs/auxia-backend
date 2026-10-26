package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignFieldReportZoneRequest(@NotNull UUID zoneId) {}
