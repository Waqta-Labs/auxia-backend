package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;

/**
 * Asks Identity Access whether a user can register a delivery now. Registering is a critical
 * command, so the role is checked when it runs, not only from the token.
 */
@FunctionalInterface
public interface AuthorizationPort {

  /** An active FIELD_BRIGADE or AUTHORITY user. */
  boolean canRegisterDeliveries(UserId userId);
}
