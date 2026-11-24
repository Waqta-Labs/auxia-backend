package com.waqtalabs.auxia.shared.infrastructure.notifications;

/**
 * Sends the e-mails of the Notification Service. The adapters of each bounded context build the
 * message; this interface only delivers it.
 *
 * <p>It never throws: an e-mail that cannot be sent is logged, so a notification never undoes the
 * action that triggered it.
 */
public interface EmailSender {

  void send(EmailMessage message);
}
