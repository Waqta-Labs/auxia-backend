package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import java.util.UUID;

/** {@code phone} is {@code null} when the member has none. */
public record StaffMemberResource(UUID id, String fullName, StaffRole role, String phone) {}
