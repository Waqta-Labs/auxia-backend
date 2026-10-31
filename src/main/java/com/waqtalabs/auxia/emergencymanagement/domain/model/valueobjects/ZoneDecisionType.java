package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/** Decision of an authority recorded in the history of a zone (US-20). */
public enum ZoneDecisionType {
  /** The authority registered the zone and became its responsible. */
  ZONE_REGISTERED,
  /** The quantities of a plan for the zone changed before deciding it (US-13, US-14). */
  PLAN_MODIFIED,
  PLAN_APPROVED,
  PLAN_REJECTED,
  /** The zone was marked as a priority by hand (US-07). */
  MANUAL_PRIORITY_SET,
  MANUAL_PRIORITY_REMOVED,
  /** The indicators of a field report of the zone were corrected (US-03). */
  REPORT_CORRECTED,
  /** The zone passed to another authority at a shift change (US-23). */
  SHIFT_HANDED_OVER
}
