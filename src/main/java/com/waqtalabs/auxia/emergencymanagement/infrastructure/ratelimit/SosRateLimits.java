package com.waqtalabs.auxia.emergencymanagement.infrastructure.ratelimit;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SosRateLimiter;

/**
 * Two limits per address: one for the SOS that each phone sends, and a wider one for the SOS that
 * a phone uploads for its neighbors after receiving them by Mesh.
 */
class SosRateLimits implements SosRateLimiter {

  private final InMemorySosRateLimiter direct;
  private final InMemorySosRateLimiter relayed;

  SosRateLimits(InMemorySosRateLimiter direct, InMemorySosRateLimiter relayed) {
    this.direct = direct;
    this.relayed = relayed;
  }

  @Override
  public void acquire(String sender, boolean relayedByMesh) {
    (relayedByMesh ? relayed : direct).acquire(sender);
  }
}
