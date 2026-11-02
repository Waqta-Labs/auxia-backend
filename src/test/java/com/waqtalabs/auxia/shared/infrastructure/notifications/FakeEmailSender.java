package com.waqtalabs.auxia.shared.infrastructure.notifications;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps the e-mails instead of sending them, so a test can read what each one says. */
public class FakeEmailSender implements EmailSender {

  private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();

  @Override
  public void send(EmailMessage message) {
    sent.add(message);
  }

  /** The e-mails sent with this subject, from the oldest. */
  public List<EmailMessage> withSubject(String subject) {
    return sent.stream().filter(message -> message.subject().equals(subject)).toList();
  }
}
