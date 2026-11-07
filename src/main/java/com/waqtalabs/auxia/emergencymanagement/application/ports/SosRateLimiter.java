package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;

/**
 * Limits how many new SOS one sender can send in a period, so the public endpoint cannot be
 * flooded. In a disaster many people share an address (the NAT of a mobile operator or the Wi-Fi
 * of a shelter), so the limit is generous.
 */
@FunctionalInterface
public interface SosRateLimiter {

  /**
   * Counts a new SOS of the sender.
   *
   * @param sender address of the client that sends the SOS
   * @throws RateLimitExceededException if the sender already reached the limit
   */
  void acquire(String sender);
}
