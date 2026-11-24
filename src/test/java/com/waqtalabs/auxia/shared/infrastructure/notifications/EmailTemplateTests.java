package com.waqtalabs.auxia.shared.infrastructure.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage.Action;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage.Detail;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage.Tone;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class EmailTemplateTests {

  private final EmailTemplate template = new EmailTemplate(texts());

  private final EmailMessage certified =
      new EmailMessage(
          Set.of("rosa@coer.gob.pe"),
          "Entrega certificada en Sector Alto Perú",
          Tone.SUCCESS,
          "Entrega certificada en blockchain",
          "La entrega en la zona Sector Alto Perú quedó certificada.",
          List.of(
              new Detail("Zona", "Sector Alto Perú"),
              new Detail("Transacción", "0xd947")),
          new Action("Ver la transacción", "https://sepolia.etherscan.io/tx/0xd947"),
          null);

  @Test
  void htmlHasTheTitleTheTableTheButtonAndTheFooter() {
    String html = template.html(certified);

    assertThat(html)
        .startsWith("<!DOCTYPE html>")
        .contains("<html lang=\"es-419\">")
        .contains("Entrega certificada en blockchain")
        .contains("Zona</td>")
        .contains("Sector Alto Perú</td>")
        .contains("href=\"https://sepolia.etherscan.io/tx/0xd947\"")
        .contains("Ver la transacción</a>")
        .contains("#05C274")
        .contains("Coordinación de ayuda humanitaria")
        .contains("Es un mensaje automático; no lo respondas.");
  }

  @Test
  void eachToneHasItsAccentColor() {
    assertThat(template.html(withTone(Tone.ALERT))).contains("#FF4D4D");
    assertThat(template.html(withTone(Tone.INFO))).contains("#4750DD");
  }

  @Test
  void valuesAreEscaped() {
    var message =
        new EmailMessage(
            Set.of("rosa@coer.gob.pe"),
            "Stock bajo",
            Tone.ALERT,
            "Stock bajo",
            "<script>alert(1)</script>",
            List.of(new Detail("Recurso", "Agua & víveres")),
            null,
            null);

    assertThat(template.html(message))
        .doesNotContain("<script>")
        .contains("&lt;script&gt;")
        .contains("Agua &amp; víveres")
        .doesNotContain("<a href");
  }

  @Test
  void textVersionHasTheSameContent() {
    assertThat(template.text(certified))
        .isEqualTo(
            """
            Entrega certificada en blockchain

            La entrega en la zona Sector Alto Perú quedó certificada.

            Zona: Sector Alto Perú
            Transacción: 0xd947

            Ver la transacción: https://sepolia.etherscan.io/tx/0xd947

            --\s
            AuxIA · Recibes este correo por tu rol en tu organización en AuxIA. \
            Es un mensaje automático; no lo respondas.""");
  }

  private EmailMessage withTone(Tone tone) {
    return new EmailMessage(
        certified.to(),
        certified.subject(),
        tone,
        certified.title(),
        certified.intro(),
        certified.details(),
        null,
        null);
  }

  private static NotificationTexts texts() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return new NotificationTexts(
        messages, new EmailProperties(true, "", 587, null, null, null, "es-419"));
  }
}
