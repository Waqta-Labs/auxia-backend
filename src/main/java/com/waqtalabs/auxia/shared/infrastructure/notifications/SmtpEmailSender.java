package com.waqtalabs.auxia.shared.infrastructure.notifications;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Sends the e-mails through an SMTP server with STARTTLS. Each e-mail goes in a virtual thread,
 * so the request that triggered it does not wait for the server.
 */
class SmtpEmailSender implements EmailSender, AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailSender.class);
  private static final String TIMEOUT_MILLIS = "10000";

  private final JavaMailSenderImpl mailSender;
  private final String from;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  SmtpEmailSender(EmailProperties properties) {
    mailSender = new JavaMailSenderImpl();
    mailSender.setHost(properties.host());
    mailSender.setPort(properties.port());
    mailSender.setUsername(properties.username());
    mailSender.setPassword(properties.password());
    mailSender.setDefaultEncoding("UTF-8");
    Properties smtp = mailSender.getJavaMailProperties();
    smtp.put("mail.smtp.auth", "true");
    smtp.put("mail.smtp.starttls.enable", "true");
    smtp.put("mail.smtp.starttls.required", "true");
    smtp.put("mail.smtp.connectiontimeout", TIMEOUT_MILLIS);
    smtp.put("mail.smtp.timeout", TIMEOUT_MILLIS);
    smtp.put("mail.smtp.writetimeout", TIMEOUT_MILLIS);
    from =
        properties.from() == null || properties.from().isBlank()
            ? properties.username()
            : properties.from();
  }

  @Override
  public void send(EmailMessage message) {
    executor.execute(() -> deliver(message));
  }

  /** One e-mail per recipient, so nobody sees the addresses of the others. */
  private void deliver(EmailMessage message) {
    for (String recipient : message.to()) {
      var email = new SimpleMailMessage();
      email.setFrom(from);
      email.setTo(recipient);
      email.setSubject(message.subject());
      email.setText(message.body());
      try {
        mailSender.send(email);
        LOGGER.info("E-mail \"{}\" sent", message.subject());
      } catch (MailException ex) {
        LOGGER.error("E-mail \"{}\" could not be sent", message.subject(), ex);
      }
    }
  }

  @Override
  public void close() {
    executor.close();
  }
}
