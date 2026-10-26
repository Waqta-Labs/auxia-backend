package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/** Coordinates of a zone, in decimal degrees. */
public record Geolocation(double latitude, double longitude) {

  public Geolocation {
    if (latitude < -90 || latitude > 90) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    if (longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
  }
}
