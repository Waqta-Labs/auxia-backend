package com.waqtalabs.auxia.shared.infrastructure.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes each e-mail to the log instead of sending it: no SMTP server, or sending is off. */
class LoggingEmailSender implements EmailSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEmailSender.class);

  @Override
  public void send(EmailMessage message) {
    LOGGER.info(
        "E-mail not sent, sending is off: \"{}\" to {} recipients",
        message.subject(),
        message.to().size());
  }
}
