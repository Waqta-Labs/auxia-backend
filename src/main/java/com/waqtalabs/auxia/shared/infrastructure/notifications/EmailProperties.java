package com.waqtalabs.auxia.shared.infrastructure.notifications;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * SMTP server of the e-mails, under {@code auxia.notifications.email}.
 *
 * @param enabled false to only log the e-mails, as the tests do
 * @param host SMTP server, such as {@code smtp.gmail.com}; blank to only log the e-mails
 * @param port port with STARTTLS, usually 587
 * @param from sender address; Gmail always sends as the signed-in account
 * @param language language tag of the texts, such as {@code es-419}
 */
@ConfigurationProperties("auxia.notifications.email")
public record EmailProperties(
    @DefaultValue("true") boolean enabled,
    String host,
    @DefaultValue("587") int port,
    String username,
    String password,
    String from,
    @DefaultValue("es-419") String language) {

  boolean sendsEmails() {
    return enabled && host != null && !host.isBlank();
  }

  public Locale locale() {
    return Locale.forLanguageTag(language);
  }
}
