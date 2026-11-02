package com.waqtalabs.auxia.shared.infrastructure.notifications;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Sends the e-mails through SMTP when a server is configured, and only logs them otherwise. */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
class EmailConfiguration {

  @Bean
  EmailSender emailSender(EmailProperties properties) {
    return properties.sendsEmails() ? new SmtpEmailSender(properties) : new LoggingEmailSender();
  }
}
