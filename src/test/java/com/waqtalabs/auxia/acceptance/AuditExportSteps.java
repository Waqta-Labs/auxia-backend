package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Steps of US-19 and US-33. The delivery is registered through the API on a plan approved for the
 * zone, and the fake blockchain confirms its anchoring; the reports are structured by the fake AI
 * Service.
 */
public class AuditExportSteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;

  private SignedInOrganization organization;
  private ApprovedPlan plan;
  private String deliveryId;
  private String emergencyId;
  private String zoneId;
  private String incompleteReportId;
  private MockHttpServletResponse response;

  @Before
  public void resetFakes() {
    aiService.reset();
    ledger.reset();
  }

  @Dado("una entrega de {int} unidades de agua registrada el {string} en la zona {string}")
  public void aRegisteredDelivery(int quantity, String date, String zone) throws Exception {
    plan = ApprovedPlan.prepare(mockMvc, aiService, zone);
    organization = plan.organization();
    deliveryId =
        JsonPath.read(
            mockMvc
                .perform(
                    get("/api/v1/deliveries")
                        .param("zoneId", plan.zoneId())
                        .with(organization.auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
    String body =
        """
        {"deliveredAt": "%sT10:30:00", "items": [{"resourceId": "%s", "quantity": %d}]}
        """
            .formatted(date, plan.water(), quantity);
    int status =
        mockMvc
            .perform(
                multipart("/api/v1/deliveries/" + deliveryId + "/registration")
                    .file(
                        new MockMultipartFile(
                            "delivery",
                            "",
                            MediaType.APPLICATION_JSON_VALUE,
                            body.getBytes(StandardCharsets.UTF_8)))
                    .file(
                        new MockMultipartFile(
                            "evidences",
                            "entrega.jpg",
                            "image/jpeg",
                            "foto de la entrega".getBytes(StandardCharsets.UTF_8)))
                    .with(organization.fieldBrigade()))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(201);
  }

  @Cuando("la autoridad exporta el historial de entregas de la zona del {string} al {string}")
  public void theAuthorityExportsTheDeliveries(String from, String to) throws Exception {
    response =
        mockMvc
            .perform(
                get("/api/v1/deliveries/report")
                    .param("zoneId", plan.zoneId())
                    .param("from", from)
                    .param("to", to)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "es-419")
                    .with(organization.authority()))
            .andReturn()
            .getResponse();
  }

  @Entonces("se genera un PDF con la entrega, sus recursos y su estado de verificación")
  public void aPdfWithTheDelivery() throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
    var reader = new PdfReader(response.getContentAsByteArray());
    String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s", "");
    assertThat(text)
        .contains("Códigopúblico" + deliveryId)
        .contains("Agua2.5L:120")
        .contains("VerificaciónVerificadaenBlockchain")
        .contains("Transacción0x");
  }

  @Entonces("el sistema indica que no hay entregas para el periodo solicitado")
  public void noDeliveriesInThePeriod() throws Exception {
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.detail"))
        .isEqualTo("No hay entregas registradas para la zona o el periodo solicitado.");
  }

  @Dado(
      "una emergencia con un reporte de la zona {string} con {int} personas y {int} familias"
          + " afectadas")
  public void anEmergencyWithAReport(String zone, int people, int families) throws Exception {
    organization = SignedInOrganization.register(mockMvc);
    emergencyId =
        idOf(
            send(
                post("/api/v1/emergencies"),
                """
                {"name": "Huaico Chosica 2026", "type": "LANDSLIDE",
                 "location": {"department": "Lima", "province": "Lima",
                              "district": "Lurigancho-Chosica"},
                 "startDate": "2026-03-14"}
                """));
    zoneId =
        idOf(
            send(
                post("/api/v1/emergencies/" + emergencyId + "/zones"),
                """
                {"name": "%s", "damageLevel": "SEVERE",
                 "populationImpact": {"familiesAffected": 5, "peopleAffected": 20}}
                """
                    .formatted(zone)));
    report("Reporte completo de " + zone, people, families);
  }

  @Dado("otro reporte de la misma zona en el que no se identificaron las familias afectadas")
  public void anIncompleteReport() throws Exception {
    incompleteReportId = report("Reporte sin familias", 60, null);
  }

  @Cuando("la autoridad exporta los reportes de la emergencia en formato SINPAD")
  public void theAuthorityExportsForSinpad() throws Exception {
    response =
        mockMvc
            .perform(
                get("/api/v1/emergencies/" + emergencyId + "/sinpad-export")
                    .with(organization.authority()))
            .andReturn()
            .getResponse();
  }

  @Entonces("se genera un archivo CSV con la estructura de campos de SINPAD")
  public void aCsvForSinpad() throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).startsWith("text/csv");
    assertThat(csvLines()[0])
        .isEqualTo(
            "EMERGENCIA,TIPO_FENOMENO,DEPARTAMENTO,PROVINCIA,DISTRITO,FECHA_INICIO,ZONA,"
                + "FECHA_REPORTE,FUENTE,NIVEL_DANO,PERSONAS_AFECTADAS,FAMILIAS_AFECTADAS,NINOS,"
                + "ADULTOS_MAYORES,PERSONAS_CON_DISCAPACIDAD,NECESIDADES");
  }

  @Y("el reporte de la zona {string} aparece con {int} personas y {int} familias afectadas")
  public void theReportIsInTheCsv(String zone, int people, int families) throws Exception {
    String[] lines = csvLines();
    assertThat(lines).hasSize(2);
    assertThat(lines[1].split(",", -1))
        .contains(zone, String.valueOf(people), String.valueOf(families));
  }

  @Entonces("el sistema indica que ese reporte debe completar {string} antes de exportar")
  public void theIncompleteReportIsListed(String field) throws Exception {
    assertThat(response.getStatus()).isEqualTo(422);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, "$.incompleteReports[*].fieldReportId"))
        .containsExactly(incompleteReportId);
    assertThat(JsonPath.<List<String>>read(body, "$.incompleteReports[0].missingFields"))
        .containsExactly(field);
  }

  private String report(String text, int people, Integer families) throws Exception {
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.SEVERE,
            families,
            people,
            null,
            null,
            null,
            Set.of(Need.WATER),
            families == null ? List.of("familiesAffected") : List.of()));
    return idOf(
        send(
            post("/api/v1/zones/" + zoneId + "/field-reports"),
            "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text)));
  }

  private String[] csvLines() throws Exception {
    return new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)
        .substring(1)
        .split("\r\n");
  }

  private MvcResult send(MockHttpServletRequestBuilder request, String body) throws Exception {
    return mockMvc
        .perform(
            request
                .with(organization.authority())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn();
  }

  private static String idOf(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
