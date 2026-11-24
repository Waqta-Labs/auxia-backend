package com.waqtalabs.auxia;

import com.waqtalabs.auxia.shared.infrastructure.notifications.FakeEmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the SMTP server with {@link FakeEmailSender} in the tests that read the notifications.
 * The other tests only log the e-mails ({@code MAIL_ENABLED=false} in Surefire).
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeEmailConfiguration {

  @Bean
  @Primary
  FakeEmailSender fakeEmailSender() {
    return new FakeEmailSender();
  }
}
