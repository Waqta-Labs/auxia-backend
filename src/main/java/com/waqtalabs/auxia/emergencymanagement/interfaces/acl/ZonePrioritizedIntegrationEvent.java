package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import java.util.UUID;

/**
 * Public version of {@code UrgencyScoreCalculated}: the zone has a score. The score itself never
 * leaves Emergency Management.
 */
public record ZonePrioritizedIntegrationEvent(UUID zoneId) {}
