package com.waqtalabs.auxia.emergencymanagement.infrastructure.export.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.context.support.ResourceBundleMessageSource;

/** The PDF of a zone includes the explanation of its score (US-05, scenario 2). */
class PdfZoneReportRendererTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final Locale SPANISH = Locale.forLanguageTag("es-419");

  private final PdfZoneReportRenderer renderer = new PdfZoneReportRenderer(messages());
  private final Emergency emergency =
      Emergency.register(
          "Huaico Chosica 2026",
          EmergencyType.LANDSLIDE,
          null,
          new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
          LocalDate.of(2026, 3, 14));

  @Test
  void reportShowsTheScoreWithThePointsOfEachVariable() throws IOException {
    Zone zone = zone(600);
    zone.updateUrgencyScore(
        new UrgencyScore(
            54.9,
            PriorityLevel.HIGH,
            List.of(
                new ScoreContribution("damageLevel", 30.0),
                new ScoreContribution("needs", 11.3),
                new ScoreContribution("vulnerablePopulation", 7.9)),
            "rules-v1",
            NOW));
    FieldReport report =
        FieldReport.register(
            emergency.id(), "Derrumbe de viviendas, urge agua.", ReportSource.AUTHORITY, NOW);
    report.markProcessed(
        new StructuredFieldData(
            DamageLevel.SEVERE,
            null,
            140,
            null,
            null,
            null,
            Set.of(Need.WATER),
            List.of("disabledPeople")));

    String text =
        textOf(renderer.render(new ZoneReport(emergency, zone, List.of(report), NOW), SPANISH));

    assertThat(text)
        .contains("Reporte de zona: Sector Alto Perú")
        .contains("Huaico Chosica 2026")
        .contains("Lurigancho-Chosica, Lima, Lima")
        .contains("Score de urgencia")
        .contains("Derrumbe de viviendas, urge agua.")
        .contains("No identificado: Personas con discapacidad");
    // The extractor may join the cells of a table row, so rows are compared without spaces.
    assertThat(text.replace(" ", ""))
        .contains("Score(0a100)54.9")
        .contains("NiveldeprioridadAlto")
        .contains("Versióndelmodelorules-v1")
        .contains("Niveldedaño30.0")
        .contains("Necesidades11.3")
        .contains("Poblaciónvulnerable7.9");
  }

  @Test
  void zoneWithoutPopulationSaysWhyItHasNoScore() throws IOException {
    String text =
        textOf(renderer.render(new ZoneReport(emergency, zone(null), List.of(), NOW), Locale.US));

    assertThat(text)
        .contains("Zone report: Sector Alto Perú")
        .contains("Not available: the population of the zone is unknown.")
        .contains("The zone has no field reports.");
  }

  private Zone zone(Integer population) {
    return emergency.registerZone(
        "Sector Alto Perú",
        null,
        new PopulationImpact(population, 35, 140, 40, 15, 0),
        DamageLevel.SEVERE);
  }

  private static String textOf(byte[] pdf) throws IOException {
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    try (var reader = new PdfReader(pdf)) {
      var extractor = new PdfTextExtractor(reader);
      var text = new StringBuilder();
      for (int page = 1; page <= reader.getNumberOfPages(); page++) {
        text.append(extractor.getTextFromPage(page)).append('\n');
      }
      return text.toString().replaceAll("\\s+", " ");
    }
  }

  private static ResourceBundleMessageSource messages() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return messages;
  }
}
