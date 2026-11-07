package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/** Coordinates of a zone or of an SOS, in decimal degrees. */
public record Geolocation(double latitude, double longitude) {

  private static final double EARTH_RADIUS_KM = 6371.0088;

  public Geolocation {
    if (latitude < -90 || latitude > 90) {
      throw new IllegalArgumentException("latitude must be between -90 and 90");
    }
    if (longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("longitude must be between -180 and 180");
    }
  }

  /** Great-circle distance to {@code other} (haversine formula). */
  public double kilometersTo(Geolocation other) {
    double latitudeDelta = Math.toRadians(other.latitude - latitude);
    double longitudeDelta = Math.toRadians(other.longitude - longitude);
    double a =
        Math.pow(Math.sin(latitudeDelta / 2), 2)
            + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
  }
}
