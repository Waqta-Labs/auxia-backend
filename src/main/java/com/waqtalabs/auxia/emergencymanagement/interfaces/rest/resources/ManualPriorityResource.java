package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

/** The zone was marked as a priority by hand (US-07). */
public record ManualPriorityResource(String reason, UUID setBy, LocalDateTime setAt) {}
