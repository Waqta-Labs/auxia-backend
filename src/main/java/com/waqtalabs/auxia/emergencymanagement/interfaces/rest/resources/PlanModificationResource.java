package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

/** Who last changed the quantities of the plan, when and why (US-14). */
public record PlanModificationResource(UUID modifiedBy, LocalDateTime modifiedAt, String reason) {}
