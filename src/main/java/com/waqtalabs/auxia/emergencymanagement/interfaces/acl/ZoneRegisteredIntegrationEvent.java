package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import java.util.UUID;

/** Public version of {@code ZoneRegistered}. */
public record ZoneRegisteredIntegrationEvent(UUID zoneId, UUID emergencyId, String name) {}
