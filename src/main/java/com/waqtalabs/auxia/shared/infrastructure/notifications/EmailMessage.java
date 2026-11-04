package com.waqtalabs.auxia.shared.infrastructure.notifications;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An e-mail to one or more recipients. The adapters give its parts; {@link EmailTemplate} lays
 * them out as HTML, with a plain text version for the clients that do not show HTML.
 *
 * @param tone what the e-mail tells, which sets its accent color
 * @param title heading of the e-mail
 * @param intro the first paragraph
 * @param details the facts of the e-mail, as a table
 * @param action a link to follow, or {@code null}
 * @param note a closing paragraph, or {@code null}
 */
public record EmailMessage(
    Set<String> to,
    String subject,
    Tone tone,
    String title,
    String intro,
    List<Detail> details,
    Action action,
    String note) {

  public EmailMessage {
    to = Set.copyOf(to);
    if (to.isEmpty()) {
      throw new IllegalArgumentException("An e-mail needs at least one recipient");
    }
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(tone, "tone");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(intro, "intro");
    details = List.copyOf(details);
  }

  /** What the e-mail tells. */
  public enum Tone {
    /** Something needs attention, such as low stock. */
    ALERT,
    /** A decision was taken, such as an approved plan. */
    INFO,
    /** Something ended well, such as a certified delivery. */
    SUCCESS
  }

  /** A row of the table of the e-mail. */
  public record Detail(String label, String value) {}

  /** A button of the e-mail. */
  public record Action(String label, String url) {}
}
