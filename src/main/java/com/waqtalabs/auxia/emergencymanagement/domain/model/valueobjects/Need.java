package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/**
 * Humanitarian need mentioned in a field report. The values are the resource categories of
 * Resource Management, so a need can later be matched with the stock that covers it.
 */
public enum Need {
  WATER,
  FOOD,
  SHELTER,
  HYGIENE,
  MEDICINE,
  OTHER
}
