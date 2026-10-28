package com.waqtalabs.auxia;

import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the {@code DeliveryRegistry} contract with {@link FakeBlockchainLedger} in the tests
 * that register deliveries. The other tests keep the real adapter, which points to an unreachable
 * network, so their anchorings fail and wait for a retry.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeBlockchainConfiguration {

  @Bean
  @Primary
  FakeBlockchainLedger fakeBlockchainLedger() {
    return new FakeBlockchainLedger();
  }
}
