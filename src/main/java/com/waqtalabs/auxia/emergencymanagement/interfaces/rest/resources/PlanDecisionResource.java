package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

/** The authority who approved or rejected the plan, and when (TS-C01). */
public record PlanDecisionResource(UUID decidedBy, LocalDateTime decidedAt) {}
