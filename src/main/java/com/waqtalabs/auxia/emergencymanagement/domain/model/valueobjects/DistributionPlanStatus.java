package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/** Life cycle of a distribution plan: PROPOSED, then APPROVED or REJECTED, then EXECUTED. */
public enum DistributionPlanStatus {
  PROPOSED,
  APPROVED,
  REJECTED,
  EXECUTED
}
