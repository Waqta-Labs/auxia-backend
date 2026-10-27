package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import java.util.UUID;

/** Quantity of a resource that a distribution plan reserves. */
public record ReservationLineResource(UUID resourceId, int quantity) {}
