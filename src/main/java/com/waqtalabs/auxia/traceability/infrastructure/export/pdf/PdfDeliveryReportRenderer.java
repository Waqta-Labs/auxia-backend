package com.waqtalabs.auxia.traceability.infrastructure.export.pdf;

import com.waqtalabs.auxia.traceability.application.ports.DeliveryReportRenderer;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryReport;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.AnchorStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Writes the history of deliveries as a PDF with OpenPDF (US-19). Labels and the verification
 * statuses come from {@code messages*.properties} under {@code deliveryReport.*}, in the language
 * of the request.
 */
@Component
class PdfDeliveryReportRenderer implements DeliveryReportRenderer {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
  private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
  private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 10);
  private static final Font CODE = FontFactory.getFont(FontFactory.COURIER, 8);
  private static final Font NOTE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

  private final MessageSource messages;

  PdfDeliveryReportRenderer(MessageSource messages) {
    this.messages = messages;
  }

  @Override
  public byte[] render(DeliveryReport report, Locale locale) {
    var labels = new Labels(locale);
    var output = new ByteArrayOutputStream();
    try (var document = new Document(PageSize.A4, 40, 40, 40, 40)) {
      PdfWriter.getInstance(document, output);
      document.open();
      document.add(new Paragraph(labels.text("title"), TITLE));
      document.add(
          new Paragraph(labels.text("generatedAt", DATE_TIME.format(report.generatedAt())), NOTE));
      addScope(document, report, labels);
      for (DeliveryReport.Entry entry : report.deliveries()) {
        addDelivery(document, entry, labels);
      }
    }
    return output.toByteArray();
  }

  private void addScope(Document document, DeliveryReport report, Labels labels) {
    document.add(heading(labels.text("scope")));
    PdfPTable table = keyValueTable();
    row(
        table,
        labels.text("zone"),
        report.zoneName() == null ? labels.text("allZones") : report.zoneName());
    row(
        table,
        labels.text("period"),
        report.from() == null && report.to() == null
            ? labels.text("allDates")
            : labels.text(
                "periodRange",
                report.from() == null ? "…" : DATE.format(report.from()),
                report.to() == null ? "…" : DATE.format(report.to())));
    long verified =
        report.deliveries().stream().filter(entry -> verified(entry.delivery())).count();
    row(table, labels.text("deliveries"), String.valueOf(report.deliveries().size()));
    row(table, labels.text("verified"), String.valueOf(verified));
    document.add(table);
  }

  private void addDelivery(Document document, DeliveryReport.Entry entry, Labels labels) {
    Delivery delivery = entry.delivery();
    Delivery.Registration registration = delivery.registration().orElseThrow();
    document.add(
        heading(
            labels.text(
                "delivery",
                DATE_TIME.format(registration.deliveredAt()),
                entry.zoneName() == null ? labels.text("unknownZone") : entry.zoneName())));
    PdfPTable table = keyValueTable();
    row(table, labels.text("publicCode"), delivery.id().value().toString());
    row(table, labels.text("deliveredAt"), DATE_TIME.format(registration.deliveredAt()));
    row(table, labels.text("receivedAt"), DATE_TIME.format(registration.receivedAt()));
    row(
        table,
        labels.text("resources"),
        entry.items().stream()
            .map(item -> item.resourceName() + ": " + item.quantity())
            .collect(Collectors.joining("\n")));
    IntegrityRecord integrity = delivery.integrity().orElseThrow();
    row(table, labels.text("verification"), labels.status(integrity.status()));
    codeRow(table, labels.text("recordHash"), integrity.recordHash().value());
    codeRow(
        table,
        labels.text("transaction"),
        integrity.transactionHash() == null ? "-" : integrity.transactionHash());
    if (integrity.anchoredAt() != null) {
      row(table, labels.text("anchoredAt"), DATE_TIME.format(integrity.anchoredAt()));
    }
    codeRow(
        table, labels.text("verifyAt"), "/api/v1/deliveries/" + delivery.id().value() + "/verify");
    document.add(table);
  }

  private static boolean verified(Delivery delivery) {
    return delivery
        .integrity()
        .map(integrity -> integrity.status() == AnchorStatus.ANCHORED)
        .orElse(false);
  }

  private static Paragraph heading(String text) {
    var paragraph = new Paragraph(text, HEADING);
    paragraph.setSpacingBefore(14);
    paragraph.setSpacingAfter(4);
    return paragraph;
  }

  private static PdfPTable keyValueTable() {
    var table = new PdfPTable(new float[] {1, 3});
    table.setWidthPercentage(100);
    return table;
  }

  private static void row(PdfPTable table, String label, String value) {
    table.addCell(cell(label, LABEL));
    table.addCell(cell(value, TEXT));
  }

  private static void codeRow(PdfPTable table, String label, String value) {
    table.addCell(cell(label, LABEL));
    table.addCell(cell(value, CODE));
  }

  private static PdfPCell cell(String text, Font font) {
    var cell = new PdfPCell(new Phrase(text, font));
    cell.setPadding(4);
    return cell;
  }

  /** Texts of the document in one language. */
  private final class Labels {

    private final Locale locale;

    private Labels(Locale locale) {
      this.locale = locale;
    }

    String text(String key, Object... arguments) {
      return messages.getMessage("deliveryReport." + key, arguments, locale);
    }

    /** What the anchoring status means for the one who reads the report. */
    String status(AnchorStatus status) {
      return messages.getMessage("deliveryReport.status." + status.name(), null, locale);
    }
  }
}
