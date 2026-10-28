package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;

/**
 * Asks Identity Access, when a plan is decided, whether the user is still an active authority:
 * the role can change after the token was issued (QAD-02).
 */
public interface AuthorizationPort {

  boolean isActiveAuthority(AuthorityId authorityId);
}
