package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/**
 * How much of what a zone needs a recommendation covers. A zone with {@code NONE} receives
 * nothing, so it gets no distribution plan (US-11, scenario 2).
 */
public enum Coverage {
  FULL,
  PARTIAL,
  NONE
}
