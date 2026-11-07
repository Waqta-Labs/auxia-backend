package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/**
 * How far a brigade is from a place and how long it takes to get there. The brigades do not
 * report their position, so the estimate goes in a straight line from their base at {@value
 * #SPEED_KMH} km/h; the mobile app uses the same speed with its own GPS.
 *
 * @param kilometers great-circle distance
 * @param minutes travel time, rounded up to the minute
 */
public record TravelEstimate(double kilometers, int minutes) {

  public static final double SPEED_KMH = 30;

  public TravelEstimate {
    if (kilometers < 0 || minutes < 0) {
      throw new IllegalArgumentException("A travel estimate must not be negative");
    }
  }

  public static TravelEstimate between(Geolocation from, Geolocation to) {
    double kilometers = from.kilometersTo(to);
    return new TravelEstimate(kilometers, (int) Math.ceil(kilometers / SPEED_KMH * 60));
  }
}
