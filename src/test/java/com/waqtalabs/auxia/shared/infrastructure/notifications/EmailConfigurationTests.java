package com.waqtalabs.auxia.shared.infrastructure.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class EmailConfigurationTests {

  private final EmailConfiguration configuration = new EmailConfiguration();

  @Test
  void configuredServerSendsTheEmailsThroughSmtp() {
    EmailSender sender = configuration.emailSender(properties(true, "smtp.gmail.com"));
    assertThat(sender).isInstanceOf(SmtpEmailSender.class);
    ((SmtpEmailSender) sender).close();
  }

  @Test
  void withoutServerTheEmailsAreOnlyLogged() {
    assertThat(configuration.emailSender(properties(true, "")))
        .isInstanceOf(LoggingEmailSender.class);
    assertThat(configuration.emailSender(properties(true, null)))
        .isInstanceOf(LoggingEmailSender.class);
  }

  @Test
  void disabledEmailsAreOnlyLoggedEvenWithServer() {
    assertThat(configuration.emailSender(properties(false, "smtp.gmail.com")))
        .isInstanceOf(LoggingEmailSender.class);
  }

  @Test
  void textsAreInTheLanguageOfTheConfiguration() {
    assertThat(properties(true, "").locale()).isEqualTo(Locale.forLanguageTag("es-419"));
  }

  private static EmailProperties properties(boolean enabled, String host) {
    return new EmailProperties(
        enabled, host, 587, "auxia@gmail.com", "app-password", "auxia@gmail.com", "es-419");
  }
}
