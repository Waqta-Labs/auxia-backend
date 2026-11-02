package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

/** A LOCKED account reached the maximum of failed sign-ins; a DISABLED one was turned off. */
public enum AccountStatus {
  ACTIVE,
  LOCKED,
  DISABLED
}
