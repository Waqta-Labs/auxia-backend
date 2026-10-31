package com.waqtalabs.auxia.emergencymanagement.infrastructure.export.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/** The SINPAD file has the structure of fields SINPAD requires (US-33, scenario 1). */
class SinpadCsvWriterTests {

  private final SinpadCsvWriter writer = new SinpadCsvWriter(messages());
  private final Emergency emergency =
      Emergency.register(
          "Huaico Chosica, marzo 2026",
          EmergencyType.LANDSLIDE,
          null,
          new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
          LocalDate.of(2026, 3, 14));

  @Test
  void oneRowPerReportWithTheValuesInSpanish() {
    FieldReport report =
        FieldReport.register(
            emergency.id(),
            "Viviendas colapsadas",
            ReportSource.AUTHORITY,
            LocalDateTime.of(2026, 3, 15, 9, 0));
    var data =
        new StructuredFieldData(
            DamageLevel.CRITICAL,
            35,
            140,
            40,
            null,
            0,
            Set.of(Need.FOOD, Need.WATER),
            List.of());

    byte[] csv =
        writer.write(
            new SinpadExport(
                emergency,
                List.of(new SinpadExport.Row(report, "Sector \"Alto\" Perú", data))));

    String text = new String(csv, StandardCharsets.UTF_8);
    assertThat(text).startsWith("﻿");
    assertThat(text.substring(1).split("\r\n"))
        .containsExactly(
            String.join(",", SinpadCsvWriter.HEADER),
            "\"Huaico Chosica, marzo 2026\",Huaico o deslizamiento,Lima,Lima,Lurigancho-Chosica,"
                + "2026-03-14,\"Sector \"\"Alto\"\" Perú\",2026-03-15 09:00,Autoridad,Crítico,"
                + "140,35,40,,0,\"Agua, Alimentos\"");
  }

  private static ResourceBundleMessageSource messages() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return messages;
  }
}
