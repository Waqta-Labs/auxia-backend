package com.waqtalabs.auxia.resourcemanagement.application.ports;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;

/** Asks Identity Access whether an account can be a member that signs in to the mobile app. */
@FunctionalInterface
public interface BrigadistAccounts {

  /** Whether it is an active FIELD_BRIGADE account of the organization. */
  boolean isActiveBrigadist(UserId userId, OrganizationId organizationId);
}
