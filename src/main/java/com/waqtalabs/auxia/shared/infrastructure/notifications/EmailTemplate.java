package com.waqtalabs.auxia.shared.infrastructure.notifications;

import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage.Detail;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage.Tone;
import java.util.stream.Collectors;
import org.springframework.web.util.HtmlUtils;

/**
 * Lays out the e-mails with the identity of AuxIA (chapter 6 of the report): the Azul Noche
 * header, an accent color for each tone, the facts in a table and a button in Azul Real. The HTML
 * uses tables and inline styles, the only layout that every e-mail client shows the same. Every
 * value is escaped.
 */
class EmailTemplate {

  private static final String NIGHT_BLUE = "#212161";
  private static final String ROYAL_BLUE = "#4750DD";
  private static final String PALE_BLUE = "#EFF0FC";
  private static final String TEXT = "#141514";
  private static final String MUTED = "#5B5F66";
  private static final String FONT = "'Inter', 'Segoe UI', Arial, sans-serif";

  private final NotificationTexts texts;

  EmailTemplate(NotificationTexts texts) {
    this.texts = texts;
  }

  String html(EmailMessage message) {
    var html = new StringBuilder();
    html.append(
        """
        <!DOCTYPE html>
        <html lang="%s"><head><meta charset="UTF-8">\
        <meta name="viewport" content="width=device-width, initial-scale=1.0">\
        <title>%s</title></head>
        <body style="margin:0;padding:0;background:#F2F3F7;">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" \
        style="background:#F2F3F7;padding:24px 12px;font-family:%s;color:%s;">
        <tr><td align="center">
        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" \
        style="max-width:560px;background:#FFFFFF;border-radius:12px;overflow:hidden;">
        <tr><td style="background:%s;padding:20px 32px;">
        <span style="font-size:22px;font-weight:700;color:#FFFFFF;letter-spacing:0.5px;">\
        AuxIA</span>
        <span style="font-size:12px;color:#85D5F6;padding-left:8px;">%s</span>
        </td></tr>
        <tr><td style="height:4px;background:%s;font-size:0;line-height:0;">&nbsp;</td></tr>
        <tr><td style="padding:32px;">
        <h1 style="margin:0 0 16px;font-size:20px;line-height:28px;color:%s;">%s</h1>
        <p style="margin:0 0 24px;font-size:15px;line-height:24px;">%s</p>
        """
            .formatted(
                texts.locale().toLanguageTag(),
                escape(message.subject()),
                FONT,
                TEXT,
                NIGHT_BLUE,
                escape(texts.get("notification.tagline")),
                accent(message.tone()),
                NIGHT_BLUE,
                escape(message.title()),
                escape(message.intro())));
    if (!message.details().isEmpty()) {
      html.append(
          """
          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" \
          style="background:%s;border-radius:8px;margin:0 0 24px;">
          """
              .formatted(PALE_BLUE));
      for (Detail detail : message.details()) {
        html.append(
            """
            <tr><td style="padding:10px 16px;font-size:13px;color:%s;width:40%%;\
            vertical-align:top;">%s</td>\
            <td style="padding:10px 16px;font-size:14px;font-weight:600;color:%s;\
            word-break:break-all;">%s</td></tr>
            """
                .formatted(MUTED, escape(detail.label()), TEXT, escape(detail.value())));
      }
      html.append("</table>\n");
    }
    if (message.action() != null) {
      html.append(
          """
          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 24px;">
          <tr><td style="background:%s;border-radius:8px;">
          <a href="%s" style="display:inline-block;padding:12px 24px;font-size:14px;\
          font-weight:600;color:#FFFFFF;text-decoration:none;">%s</a>
          </td></tr></table>
          """
              .formatted(
                  ROYAL_BLUE,
                  escape(message.action().url()),
                  escape(message.action().label())));
    }
    if (message.note() != null) {
      html.append(
          """
          <p style="margin:0;font-size:14px;line-height:22px;color:%s;">%s</p>
          """
              .formatted(MUTED, escape(message.note())));
    }
    html.append(
        """
        </td></tr>
        <tr><td style="padding:20px 32px;border-top:1px solid #E6EAE4;font-size:12px;\
        line-height:18px;color:%s;">%s</td></tr>
        </table>
        </td></tr></table>
        </body></html>
        """
            .formatted(MUTED, escape(texts.get("notification.footer"))));
    return html.toString();
  }

  /** The same e-mail for the clients that do not show HTML. */
  String text(EmailMessage message) {
    var text = new StringBuilder();
    text.append(message.title()).append("\n\n").append(message.intro()).append("\n\n");
    if (!message.details().isEmpty()) {
      text.append(
              message.details().stream()
                  .map(detail -> detail.label() + ": " + detail.value())
                  .collect(Collectors.joining("\n")))
          .append("\n\n");
    }
    if (message.action() != null) {
      text.append(message.action().label())
          .append(": ")
          .append(message.action().url())
          .append("\n\n");
    }
    if (message.note() != null) {
      text.append(message.note()).append("\n\n");
    }
    return text.append("-- \nAuxIA · ").append(texts.get("notification.footer")).toString();
  }

  private static String accent(Tone tone) {
    return switch (tone) {
      case ALERT -> "#FF4D4D";
      case INFO -> ROYAL_BLUE;
      case SUCCESS -> "#05C274";
    };
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value, "UTF-8");
  }
}
