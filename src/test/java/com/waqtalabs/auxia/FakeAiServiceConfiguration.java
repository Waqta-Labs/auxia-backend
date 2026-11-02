package com.waqtalabs.auxia;

import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the AI Service gateways with {@link FakeAiService} in the tests that need structured
 * reports or urgency scores. The other tests keep the real gateways, which point to an
 * unreachable URL.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeAiServiceConfiguration {

  @Bean
  @Primary
  FakeAiService fakeAiService() {
    return new FakeAiService();
  }
}
