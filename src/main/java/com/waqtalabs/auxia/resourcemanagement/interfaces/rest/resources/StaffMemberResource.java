package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import java.util.UUID;

/** {@code phone} and {@code userId} are {@code null} when the member has none. */
public record StaffMemberResource(
    UUID id, String fullName, StaffRole role, String phone, UUID userId) {}
