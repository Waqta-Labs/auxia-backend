package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import java.util.UUID;

/** A brigade and the organization it belongs to. */
public record BrigadeReferenceResource(UUID brigadeId, UUID organizationId, String name) {}
