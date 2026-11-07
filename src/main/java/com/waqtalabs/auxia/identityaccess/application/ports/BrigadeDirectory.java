package com.waqtalabs.auxia.identityaccess.application.ports;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import java.util.Optional;
import java.util.UUID;

/** The brigades of Resource Management, for the registration of brigadists (A_02). */
public interface BrigadeDirectory {

  /** The brigade with the code, ignoring case; empty when none has it. */
  Optional<BrigadeReference> findByCode(String code);

  /** Adds the approved brigadist to the brigade, linked to their account. */
  void addBrigadist(
      UUID brigadeId, OrganizationId organizationId, String fullName, String phone, UserId user);

  /** A brigade and its organization. */
  record BrigadeReference(UUID brigadeId, OrganizationId organizationId) {}
}
