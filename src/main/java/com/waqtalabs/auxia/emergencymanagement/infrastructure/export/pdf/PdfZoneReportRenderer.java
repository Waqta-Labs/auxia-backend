package com.waqtalabs.auxia.emergencymanagement.infrastructure.export.pdf;

import com.waqtalabs.auxia.emergencymanagement.application.ports.ZoneReportRenderer;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
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
 * Writes the report of a zone as a PDF with OpenPDF. Labels and enumeration values come from
 * {@code messages*.properties} under {@code zoneReport.*}, in the language of the request.
 */
@Component
class PdfZoneReportRenderer implements ZoneReportRenderer {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
  private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
  private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 10);
  private static final Font NOTE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

  private final MessageSource messages;

  PdfZoneReportRenderer(MessageSource messages) {
    this.messages = messages;
  }

  @Override
  public byte[] render(ZoneReport report, Locale locale) {
    var labels = new Labels(locale);
    var output = new ByteArrayOutputStream();
    try (var document = new Document(PageSize.A4, 40, 40, 40, 40)) {
      PdfWriter.getInstance(document, output);
      document.open();
      Zone zone = report.zone();
      document.add(new Paragraph(labels.text("title", zone.name()), TITLE));
      document.add(
          new Paragraph(labels.text("generatedAt", format(report.generatedAt(), DATE_TIME)), NOTE));
      addEmergency(document, report.emergency(), labels);
      addZone(document, zone, labels);
      addUrgencyScore(document, zone, labels);
      addFieldReports(document, report, labels);
    }
    return output.toByteArray();
  }

  private void addEmergency(Document document, Emergency emergency, Labels labels) {
    document.add(heading(labels.text("emergency")));
    PdfPTable table = keyValueTable();
    row(table, labels.text("name"), emergency.name());
    row(table, labels.text("type"), labels.value(emergency.type()));
    row(table, labels.text("location"), emergency.location().fullName());
    row(table, labels.text("startDate"), format(emergency.startDate(), DATE));
    row(table, labels.text("status"), labels.value(emergency.status()));
    document.add(table);
  }

  private void addZone(Document document, Zone zone, Labels labels) {
    document.add(heading(labels.text("zone")));
    PdfPTable table = keyValueTable();
    row(table, labels.text("status"), labels.value(zone.status()));
    row(table, labels.text("damageLevel"), labels.value(zone.damageLevel()));
    row(
        table,
        labels.text("geolocation"),
        zone.geolocation()
            .map(location -> location.latitude() + ", " + location.longitude())
            .orElse("-"));
    PopulationImpact impact = zone.populationImpact();
    row(
        table,
        labels.text("population"),
        impact.knownPopulation().map(String::valueOf).orElse(labels.text("unknown")));
    row(table, labels.text("familiesAffected"), String.valueOf(impact.familiesAffected()));
    row(table, labels.text("peopleAffected"), String.valueOf(impact.peopleAffected()));
    row(table, labels.text("children"), String.valueOf(impact.children()));
    row(table, labels.text("elderlyPeople"), String.valueOf(impact.elderlyPeople()));
    row(table, labels.text("disabledPeople"), String.valueOf(impact.disabledPeople()));
    document.add(table);
  }

  private void addUrgencyScore(Document document, Zone zone, Labels labels) {
    document.add(heading(labels.text("urgencyScore")));
    if (zone.urgencyScore().isEmpty()) {
      String reason = zone.hasSufficientDataForScoring() ? "scorePending" : "scoreNotAvailable";
      document.add(new Paragraph(labels.text(reason), TEXT));
      return;
    }
    UrgencyScore score = zone.urgencyScore().get();
    PdfPTable summary = keyValueTable();
    row(summary, labels.text("score"), String.valueOf(score.value()));
    row(summary, labels.text("priorityLevel"), labels.value(score.level()));
    row(summary, labels.text("modelVersion"), score.modelVersion());
    row(summary, labels.text("calculatedAt"), format(score.calculatedAt(), DATE_TIME));
    document.add(summary);

    document.add(new Paragraph(labels.text("topFeatures"), LABEL));
    var features = new PdfPTable(new float[] {3, 1});
    features.setWidthPercentage(100);
    features.setSpacingBefore(4);
    features.addCell(cell(labels.text("variable"), LABEL));
    features.addCell(cell(labels.text("contribution"), LABEL));
    for (ScoreContribution contribution : score.topFeatures()) {
      features.addCell(cell(labels.feature(contribution.feature()), TEXT));
      PdfPCell points = cell(String.valueOf(contribution.contribution()), TEXT);
      points.setHorizontalAlignment(Element.ALIGN_RIGHT);
      features.addCell(points);
    }
    document.add(features);
  }

  private void addFieldReports(Document document, ZoneReport report, Labels labels) {
    document.add(heading(labels.text("fieldReports", report.fieldReports().size())));
    if (report.fieldReports().isEmpty()) {
      document.add(new Paragraph(labels.text("noFieldReports"), TEXT));
      return;
    }
    for (FieldReport fieldReport : report.fieldReports()) {
      String header =
          format(fieldReport.reportedAt(), DATE_TIME)
              + " · "
              + labels.value(fieldReport.source())
              + " · "
              + labels.text(fieldReport.isProcessed() ? "processed" : "notProcessed");
      var paragraph = new Paragraph(header, LABEL);
      paragraph.setSpacingBefore(6);
      document.add(paragraph);
      document.add(new Paragraph(fieldReport.rawText(), TEXT));
      fieldReport
          .structuredData()
          .filter(data -> data.hasMissingVariables())
          .ifPresent(
              data ->
                  document.add(
                      new Paragraph(
                          labels.text(
                              "missingVariables",
                              String.join(
                                  ", ",
                                  data.missingVariables().stream().map(labels::feature).toList())),
                          NOTE)));
    }
  }

  private static Paragraph heading(String text) {
    var paragraph = new Paragraph(text, HEADING);
    paragraph.setSpacingBefore(14);
    paragraph.setSpacingAfter(4);
    return paragraph;
  }

  private static PdfPTable keyValueTable() {
    var table = new PdfPTable(new float[] {1, 2});
    table.setWidthPercentage(100);
    return table;
  }

  private static void row(PdfPTable table, String label, String value) {
    table.addCell(cell(label, LABEL));
    table.addCell(cell(value, TEXT));
  }

  private static PdfPCell cell(String text, Font font) {
    var cell = new PdfPCell(new Phrase(text, font));
    cell.setPadding(4);
    return cell;
  }

  private static String format(TemporalAccessor value, DateTimeFormatter formatter) {
    return formatter.format(value);
  }

  /** Texts of the document in one language. */
  private final class Labels {

    private final Locale locale;

    private Labels(Locale locale) {
      this.locale = locale;
    }

    String text(String key, Object... arguments) {
      return messages.getMessage("zoneReport." + key, arguments, locale);
    }

    /** An enumeration value; without a translation, its name. */
    String value(Enum<?> value) {
      return messages.getMessage("zoneReport.value." + value.name(), null, value.name(), locale);
    }

    /** A variable of the score or of a field report; without a translation, its name. */
    String feature(String name) {
      return messages.getMessage("zoneReport.feature." + name, null, name, locale);
    }
  }
}
