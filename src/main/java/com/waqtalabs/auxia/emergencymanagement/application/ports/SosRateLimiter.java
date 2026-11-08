package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;

/**
 * Limits how many new SOS one sender can send in a period, so the public endpoint cannot be
 * flooded. In a disaster many people share an address (the NAT of a mobile operator or the Wi-Fi
 * of a shelter), so the limit is generous. An SOS relayed by Mesh counts in a wider limit of its
 * own: one phone with signal uploads the SOS of many neighbors.
 */
@FunctionalInterface
public interface SosRateLimiter {

  /**
   * Counts a new SOS of the sender.
   *
   * @param sender address of the client that sends the SOS
   * @param relayed whether the phone uploads an SOS that another phone passed to it by Mesh
   * @throws RateLimitExceededException if the sender already reached the limit
   */
  void acquire(String sender, boolean relayed);
}
