package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

/** Vulnerable people that need help, according to the SOS of a citizen. */
public record VulnerablePeople(int children, int elderly, int injured) {

  public static final VulnerablePeople NONE = new VulnerablePeople(0, 0, 0);

  public VulnerablePeople {
    if (children < 0 || elderly < 0 || injured < 0) {
      throw new IllegalArgumentException("The number of people must not be negative");
    }
  }
}
