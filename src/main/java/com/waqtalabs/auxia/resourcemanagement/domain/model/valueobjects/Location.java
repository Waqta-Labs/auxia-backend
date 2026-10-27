package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

/**
 * Coordinates of the base of a brigade. The brigade suggested for a zone is the closest one, so
 * the location knows its distance to another point.
 */
public record Location(double latitude, double longitude) {

  private static final double EARTH_RADIUS_KM = 6371.0;

  public Location {
    if (latitude < -90 || latitude > 90) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    if (longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
  }

  /** Great-circle distance in kilometers (haversine formula). */
  public double distanceTo(Location other) {
    double latitudeDelta = Math.toRadians(other.latitude - latitude);
    double longitudeDelta = Math.toRadians(other.longitude - longitude);
    double a =
        Math.pow(Math.sin(latitudeDelta / 2), 2)
            + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
  }
}
