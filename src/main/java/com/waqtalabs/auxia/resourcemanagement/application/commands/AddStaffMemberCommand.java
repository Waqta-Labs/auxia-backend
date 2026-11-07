package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;

/**
 * Adds a member to a brigade.
 *
 * @param phone optional
 * @param userId FIELD_BRIGADE account of the member in the mobile app, or {@code null}
 */
public record AddStaffMemberCommand(
    BrigadeId brigadeId,
    OrganizationId organizationId,
    String fullName,
    StaffRole role,
    String phone,
    UserId userId) {

  /** A member without an account. */
  public AddStaffMemberCommand(
      BrigadeId brigadeId,
      OrganizationId organizationId,
      String fullName,
      StaffRole role,
      String phone) {
    this(brigadeId, organizationId, fullName, role, phone, null);
  }
}
