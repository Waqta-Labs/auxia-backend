package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;

/**
 * Adds a member to a brigade.
 *
 * @param phone optional
 */
public record AddStaffMemberCommand(
    BrigadeId brigadeId, String fullName, StaffRole role, String phone) {}
