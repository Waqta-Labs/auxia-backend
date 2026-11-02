package com.waqtalabs.auxia.sinpadsimulator;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The registrations received by the simulator, in memory: they are lost when the backend restarts,
 * as the simulator only stands in for SINPAD. An emergency keeps its registration number when it
 * is sent again, and its reports are replaced.
 */
@Component
@ConditionalOnProperty(
    name = "auxia.sinpad.simulator.enabled",
    havingValue = "true",
    matchIfMissing = true)
class SinpadRegistry {

  static final String RECEIVED = "RECIBIDO";

  private final Map<String, Registration> registrationsByExternalCode = new ConcurrentHashMap<>();
  private final Map<String, Registration> registrationsByCode = new ConcurrentHashMap<>();
  private final AtomicInteger sequence = new AtomicInteger();
  private final Clock clock;

  SinpadRegistry(Clock clock) {
    this.clock = clock;
  }

  /** Registers the reports of the emergency, or replaces them if it was received before. */
  synchronized Registration receive(SinpadSubmission submission) {
    LocalDateTime now = LocalDateTime.now(clock);
    Registration previous = registrationsByExternalCode.get(submission.codigoExterno());
    String code =
        previous != null
            ? previous.receipt().codigoRegistro()
            : "SINPAD-%d-%06d".formatted(now.getYear(), sequence.incrementAndGet());
    var registration =
        new Registration(
            new SinpadReceipt(
                code,
                submission.codigoExterno(),
                RECEIVED,
                submission.reportes().size(),
                now),
            submission,
            previous == null);
    registrationsByExternalCode.put(submission.codigoExterno(), registration);
    registrationsByCode.put(code, registration);
    return registration;
  }

  Optional<Registration> find(String code) {
    return Optional.ofNullable(registrationsByCode.get(code));
  }

  /**
   * A registration with the reports it holds.
   *
   * @param isNew false when the emergency had been received before
   */
  record Registration(SinpadReceipt receipt, SinpadSubmission submission, boolean isNew) {}
}
