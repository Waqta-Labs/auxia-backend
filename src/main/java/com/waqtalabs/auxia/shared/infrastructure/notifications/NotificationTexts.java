package com.waqtalabs.auxia.shared.infrastructure.notifications;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Texts of the e-mails, from {@code messages*.properties} in the language of the notifications.
 * An e-mail has no request, so it cannot follow the Accept-Language of the user.
 */
@Component
public class NotificationTexts {

  private final MessageSource messages;
  private final EmailProperties properties;

  NotificationTexts(MessageSource messages, EmailProperties properties) {
    this.messages = messages;
    this.properties = properties;
  }

  public String get(String code, Object... arguments) {
    return messages.getMessage(code, arguments, properties.locale());
  }
}
