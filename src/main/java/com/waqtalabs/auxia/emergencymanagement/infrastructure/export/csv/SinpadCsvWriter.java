package com.waqtalabs.auxia.emergencymanagement.infrastructure.export.csv;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SinpadExportWriter;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Writes the SINPAD export as CSV: UTF-8 with a byte order mark, so Excel opens the accents well,
 * one row per field report and the values in Spanish, as SINPAD shows them (US-33). The location
 * of the emergency goes by the names of its department, province and district. A count the report
 * does not have stays empty.
 */
@Component
class SinpadCsvWriter implements SinpadExportWriter {

  static final List<String> HEADER =
      List.of(
          "EMERGENCIA",
          "TIPO_FENOMENO",
          "DEPARTAMENTO",
          "PROVINCIA",
          "DISTRITO",
          "FECHA_INICIO",
          "ZONA",
          "FECHA_REPORTE",
          "FUENTE",
          "NIVEL_DANO",
          "PERSONAS_AFECTADAS",
          "FAMILIAS_AFECTADAS",
          "NINOS",
          "ADULTOS_MAYORES",
          "PERSONAS_CON_DISCAPACIDAD",
          "NECESIDADES");

  private static final Locale SPANISH = Locale.forLanguageTag("es-419");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final String BYTE_ORDER_MARK = "﻿";
  private static final String LINE = "\r\n";

  private final MessageSource messages;

  SinpadCsvWriter(MessageSource messages) {
    this.messages = messages;
  }

  @Override
  public byte[] write(SinpadExport export) {
    var csv = new StringBuilder(BYTE_ORDER_MARK).append(line(HEADER.stream()));
    Emergency emergency = export.emergency();
    AdministrativeLocation location = emergency.location();
    for (SinpadExport.Row row : export.rows()) {
      FieldReport report = row.report();
      StructuredFieldData data = row.data();
      csv.append(
          line(
              Stream.of(
                  emergency.name(),
                  spanish(emergency.type()),
                  location.department(),
                  location.province(),
                  location.district(),
                  DATE.format(emergency.startDate()),
                  row.zoneName(),
                  DATE_TIME.format(report.reportedAt()),
                  spanish(report.source()),
                  spanish(data.damageLevel()),
                  count(data.peopleAffected()),
                  count(data.familiesAffected()),
                  count(data.children()),
                  count(data.elderlyPeople()),
                  count(data.disabledPeople()),
                  data.needs().stream().map(this::spanish).collect(Collectors.joining(", ")))));
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  /** The translation of the zone report; without one, the name of the value. */
  private String spanish(Enum<?> value) {
    return messages.getMessage("zoneReport.value." + value.name(), null, value.name(), SPANISH);
  }

  private static String count(Integer value) {
    return value == null ? "" : value.toString();
  }

  private static String line(Stream<String> values) {
    return values.map(SinpadCsvWriter::quoted).collect(Collectors.joining(",")) + LINE;
  }

  /** RFC 4180: a value with a comma, a quote or a line break goes between quotes. */
  private static String quoted(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
