package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import java.util.UUID;

/** Quantity of a resource that an approved distribution plan delivers. */
public record PlannedQuantityResource(UUID resourceId, int quantity) {}
