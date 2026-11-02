package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

/** A reservation stays ACTIVE until it is consumed by a delivery or released. */
public enum ReservationStatus {
  ACTIVE,
  CONSUMED,
  RELEASED
}
