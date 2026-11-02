package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.sinpad;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SinpadGateway;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Anti-Corruption Layer to {@code POST /api/v1/reportes} of SINPAD. It translates the export into
 * the contract of SINPAD, with the fields and values in Spanish as the export file has them, and
 * the answer into a {@link SinpadRegistration}. The emergency goes by its id, so sending it again
 * replaces its reports. INDECI has no public API: {@code SINPAD_URL} points to the simulator of
 * this backend unless it is set to another service.
 */
@Component
class RestSinpadGateway implements SinpadGateway {

  static final String ENDPOINT = "/api/v1/reportes";
  static final String SERVICE_NAME = "SINPAD";

  private static final Logger LOGGER = LoggerFactory.getLogger(RestSinpadGateway.class);
  private static final Locale SPANISH = Locale.forLanguageTag("es-419");

  private final RestClient restClient;
  private final MessageSource messages;

  RestSinpadGateway(SinpadProperties properties, MessageSource messages) {
    var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    this.restClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeader("X-Api-Key", properties.apiKey())
            .requestFactory(requestFactory)
            .build();
    this.messages = messages;
  }

  @Override
  public SinpadRegistration submit(SinpadExport export) {
    Receipt receipt;
    try {
      receipt =
          restClient.post().uri(ENDPOINT).body(request(export)).retrieve().body(Receipt.class);
    } catch (RestClientException ex) {
      LOGGER.warn("Call to {} {} failed: {}", SERVICE_NAME, ENDPOINT, ex.getMessage());
      throw new ExternalServiceUnavailableException(SERVICE_NAME, ex);
    }
    if (receipt == null || receipt.codigoRegistro() == null) {
      LOGGER.warn("Call to {} {} returned no registration", SERVICE_NAME, ENDPOINT);
      throw new ExternalServiceUnavailableException(SERVICE_NAME, null);
    }
    return new SinpadRegistration(
        receipt.codigoRegistro(), receipt.reportesRecibidos(), receipt.recibidoEn());
  }

  private Submission request(SinpadExport export) {
    Emergency emergency = export.emergency();
    AdministrativeLocation location = emergency.location();
    return new Submission(
        emergency.id().value().toString(),
        new Submission.Emergencia(
            emergency.name(),
            spanish(emergency.type()),
            location.department(),
            location.province(),
            location.district(),
            emergency.startDate()),
        export.rows().stream().map(this::report).toList());
  }

  private Submission.Reporte report(SinpadExport.Row row) {
    StructuredFieldData data = row.data();
    return new Submission.Reporte(
        row.zoneName(),
        row.report().reportedAt(),
        spanish(row.report().source()),
        spanish(data.damageLevel()),
        data.peopleAffected(),
        data.familiesAffected(),
        data.children(),
        data.elderlyPeople(),
        data.disabledPeople(),
        data.needs().stream().map(this::spanish).sorted().toList());
  }

  /** The value as SINPAD shows it, the same translation as the export file. */
  private String spanish(Enum<?> value) {
    return messages.getMessage("zoneReport.value." + value.name(), null, value.name(), SPANISH);
  }

  /** The request of SINPAD. Its names are the ones of the contract, in Spanish. */
  record Submission(String codigoExterno, Emergencia emergencia, List<Reporte> reportes) {

    record Emergencia(
        String nombre,
        String tipoFenomeno,
        String departamento,
        String provincia,
        String distrito,
        LocalDate fechaInicio) {}

    record Reporte(
        String zona,
        LocalDateTime fechaReporte,
        String fuente,
        String nivelDano,
        Integer personasAfectadas,
        Integer familiasAfectadas,
        Integer ninos,
        Integer adultosMayores,
        Integer personasConDiscapacidad,
        List<String> necesidades) {}
  }

  /** The answer of SINPAD. */
  record Receipt(String codigoRegistro, int reportesRecibidos, LocalDateTime recibidoEn) {}
}
