package com.waqtalabs.auxia.shared.infrastructure.notifications;

import java.util.Objects;
import java.util.Set;

/** A plain text e-mail to one or more recipients. */
public record EmailMessage(Set<String> to, String subject, String body) {

  public EmailMessage {
    to = Set.copyOf(to);
    if (to.isEmpty()) {
      throw new IllegalArgumentException("An e-mail needs at least one recipient");
    }
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(body, "body");
  }
}
