package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

/** Role of a user. It decides which actions the user can execute (US-22). */
public enum Role {
  ORGANIZATION_ADMIN,
  AUTHORITY,
  FIELD_BRIGADE,
  AUDITOR
}
