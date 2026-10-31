package com.waqtalabs.auxia.traceability.infrastructure.export.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.traceability.application.Sha256;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryReport;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.DeliveryItem;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * The PDF of the deliveries shows, for each one, its date, resources, hash, transaction and
 * verification status (US-19, scenario 1).
 */
class PdfDeliveryReportRendererTests {

  private static final Locale SPANISH = Locale.forLanguageTag("es-419");
  private static final String TRANSACTION = "0x" + "c".repeat(64);

  private final PdfDeliveryReportRenderer renderer = new PdfDeliveryReportRenderer(messages());

  @Test
  void eachDeliveryShowsItsResourcesHashTransactionAndVerification() throws IOException {
    Delivery certified = registered(LocalDateTime.of(2026, 3, 15, 10, 30));
    certified.markSubmitted(TRANSACTION);
    certified.markAnchored(LocalDateTime.of(2026, 3, 15, 10, 35));
    Delivery pending = registered(LocalDateTime.of(2026, 3, 16, 9, 0));
    var report =
        new DeliveryReport(
            "Sector Alto Perú",
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 3, 16),
            LocalDateTime.of(2026, 3, 20, 12, 0),
            List.of(
                new DeliveryReport.Entry(
                    certified,
                    "Sector Alto Perú",
                    List.of(
                        new DeliveryReport.Item("Agua 2.5 L", 120),
                        new DeliveryReport.Item("Kit de alimentos", 80))),
                new DeliveryReport.Entry(
                    pending,
                    "Sector Alto Perú",
                    List.of(new DeliveryReport.Item("Agua 2.5 L", 40)))));

    String text = textOf(renderer.render(report, SPANISH));

    assertThat(text)
        .contains("Historialdeentregas")
        .contains("Del2026-03-15al2026-03-16")
        .contains("Entregas2")
        .contains("VerificadasenBlockchain1")
        .contains("Entregadel2026-03-1510:30·SectorAltoPerú")
        .contains("Códigopúblico" + certified.id().value())
        .contains("Agua2.5L:120")
        .contains("Kitdealimentos:80")
        .contains("VerificaciónVerificadaenBlockchain")
        .contains(certified.integrity().orElseThrow().recordHash().value())
        .contains(TRANSACTION)
        .contains("/api/v1/deliveries/" + certified.id().value() + "/verify")
        .contains("VerificaciónPendientedeverificación");
  }

  @Test
  void aReportOfEveryZoneAndDateSaysSo() throws IOException {
    var report =
        new DeliveryReport(
            null,
            null,
            null,
            LocalDateTime.of(2026, 3, 20, 12, 0),
            List.of(
                new DeliveryReport.Entry(
                    registered(LocalDateTime.of(2026, 3, 15, 10, 30)),
                    null,
                    List.of(new DeliveryReport.Item("Agua 2.5 L", 120)))));

    String text = textOf(renderer.render(report, Locale.ENGLISH));

    assertThat(text)
        .contains("Historyofdeliveries")
        .contains("ZoneEveryzone")
        .contains("PeriodEverydate")
        .contains("Unknownzone")
        .contains("Pendingverification");
  }

  private static Delivery registered(LocalDateTime deliveredAt) {
    Delivery delivery =
        Delivery.initiate(
            new DistributionPlanId(UUID.randomUUID()),
            new ZoneId(UUID.randomUUID()),
            new BrigadeId(UUID.randomUUID()),
            new OrganizationId(UUID.randomUUID()));
    delivery.register(
        List.of(DeliveryItem.of(new ResourceId(UUID.randomUUID()), 120)),
        List.of(
            Evidence.attach(
                EvidenceType.PHOTO,
                "https://storage/evidences/a",
                new HashValue("a".repeat(64)),
                deliveredAt,
                null)),
        new UserId(UUID.randomUUID()),
        deliveredAt,
        LocalDateTime.of(2026, 3, 20, 8, 0),
        new Sha256());
    return delivery;
  }

  private static String textOf(byte[] pdf) throws IOException {
    var reader = new PdfReader(pdf);
    var extractor = new PdfTextExtractor(reader);
    var text = new StringBuilder();
    for (int page = 1; page <= reader.getNumberOfPages(); page++) {
      text.append(extractor.getTextFromPage(page));
    }
    return text.toString().replaceAll("\\s", "");
  }

  private static ResourceBundleMessageSource messages() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return messages;
  }
}
