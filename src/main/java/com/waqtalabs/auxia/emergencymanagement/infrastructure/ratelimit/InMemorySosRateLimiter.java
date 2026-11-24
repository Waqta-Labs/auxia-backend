package com.waqtalabs.auxia.emergencymanagement.infrastructure.ratelimit;

import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Sliding window in memory: an address may send {@code maxReports} new SOS in any period of
 * {@code window}. The API runs as a single instance, so the memory of the process is enough; a
 * restart forgets the counts, which only makes the limit more lenient.
 */
class InMemorySosRateLimiter {

  /** Above this many addresses, the ones without recent SOS are forgotten. */
  private static final int SENDERS_BEFORE_CLEANUP = 10_000;

  private final int maxReports;
  private final Duration window;
  private final Clock clock;
  private final Map<String, Deque<Instant>> sentBySender = new HashMap<>();

  InMemorySosRateLimiter(int maxReports, Duration window, Clock clock) {
    this.maxReports = maxReports;
    this.window = window;
    this.clock = clock;
  }

  synchronized void acquire(String sender) {
    Instant now = clock.instant();
    Instant windowStart = now.minus(window);
    if (sentBySender.size() > SENDERS_BEFORE_CLEANUP) {
      sentBySender.values().forEach(sent -> forgetBefore(sent, windowStart));
      sentBySender.values().removeIf(Deque::isEmpty);
    }
    Deque<Instant> sent = sentBySender.computeIfAbsent(sender, ignored -> new ArrayDeque<>());
    forgetBefore(sent, windowStart);
    if (sent.size() >= maxReports) {
      Duration untilOldestLeaves = Duration.between(now, sent.peekFirst().plus(window));
      throw new RateLimitExceededException(untilOldestLeaves);
    }
    sent.addLast(now);
  }

  private static void forgetBefore(Deque<Instant> sent, Instant windowStart) {
    while (!sent.isEmpty() && !sent.peekFirst().isAfter(windowStart)) {
      sent.removeFirst();
    }
  }
}
