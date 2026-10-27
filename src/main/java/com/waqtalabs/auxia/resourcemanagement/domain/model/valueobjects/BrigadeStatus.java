package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

/** Availability of a brigade. It is ASSIGNED only while it has an assignment in progress. */
public enum BrigadeStatus {
  AVAILABLE,
  ASSIGNED,
  OFF_DUTY
}
