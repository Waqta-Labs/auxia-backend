package com.waqtalabs.auxia.emergencymanagement.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemorySosRateLimiterTests {

  private final MovingClock clock = new MovingClock();
  private final InMemorySosRateLimiter limiter =
      new InMemorySosRateLimiter(3, Duration.ofMinutes(10), clock);

  @Test
  void senderOverTheLimitWaitsUntilItsOldestSosLeavesTheWindow() {
    limiter.acquire("190.12.4.7");
    clock.advance(Duration.ofMinutes(4));
    limiter.acquire("190.12.4.7");
    limiter.acquire("190.12.4.7");

    assertThatThrownBy(() -> limiter.acquire("190.12.4.7"))
        .isInstanceOf(RateLimitExceededException.class)
        .extracting(ex -> ((RateLimitExceededException) ex).retryAfter())
        .isEqualTo(Duration.ofMinutes(6));

    clock.advance(Duration.ofMinutes(6));
    assertThatCode(() -> limiter.acquire("190.12.4.7")).doesNotThrowAnyException();
  }

  @Test
  void eachSenderHasItsOwnLimit() {
    limiter.acquire("190.12.4.7");
    limiter.acquire("190.12.4.7");
    limiter.acquire("190.12.4.7");

    assertThatCode(() -> limiter.acquire("181.65.2.33")).doesNotThrowAnyException();
  }

  @Test
  void rejectedSosDoNotCount() {
    limiter.acquire("190.12.4.7");
    limiter.acquire("190.12.4.7");
    limiter.acquire("190.12.4.7");
    for (int attempt = 0; attempt < 5; attempt++) {
      clock.advance(Duration.ofMinutes(1));
      assertThatThrownBy(() -> limiter.acquire("190.12.4.7"))
          .isInstanceOf(RateLimitExceededException.class);
    }

    clock.advance(Duration.ofMinutes(5));

    assertThatCode(() -> limiter.acquire("190.12.4.7")).doesNotThrowAnyException();
  }

  /** A clock that the test moves forward. */
  private static final class MovingClock extends Clock {

    private Instant now = Instant.parse("2026-11-06T14:41:05Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }
}
