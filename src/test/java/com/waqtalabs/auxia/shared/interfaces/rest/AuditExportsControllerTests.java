package com.waqtalabs.auxia.shared.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.FakeBlockchainConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Exports through the API: the history of deliveries for an audit (US-19) and the field reports
 * for SINPAD (US-33).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  FakeBlockchainConfiguration.class
})
class AuditExportsControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;

  @BeforeEach
  void resetFakes() {
    aiService.reset();
    ledger.reset();
  }

  @Test
  void theDeliveriesOfAZoneAreExportedAsPdf() throws Exception {
    ApprovedPlan plan = ApprovedPlan.prepare(mockMvc, aiService);
    SignedInOrganization organization = plan.organization();
    String delivery = registerDelivery(plan);

    byte[] pdf =
        mockMvc
            .perform(
                get("/api/v1/deliveries/report")
                    .param("zoneId", plan.zoneId())
                    .param("from", "2026-03-15")
                    .param("to", "2026-03-15")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "es-419")
                    .with(organization.auditor()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"deliveries.pdf\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(textOf(pdf))
        .contains("SectorAltoPerú")
        .contains("Códigopúblico" + delivery)
        .contains("Agua2.5L:120")
        .contains("Kitdealimentos:80")
        .contains("VerificaciónVerificadaenBlockchain");
  }

  @Test
  void withoutDeliveriesInThePeriodTheReportSaysSo() throws Exception {
    ApprovedPlan plan = ApprovedPlan.prepare(mockMvc, aiService);
    registerDelivery(plan);

    mockMvc
        .perform(
            get("/api/v1/deliveries/report")
                .param("from", "2026-04-01")
                .with(plan.organization().auditor()))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.detail")
                .value("There are no deliveries registered for the zone or period requested."));
    mockMvc
        .perform(
            get("/api/v1/deliveries/report")
                .param("from", "2026-03-16")
                .param("to", "2026-03-15")
                .with(plan.organization().auditor()))
        .andExpect(status().isUnprocessableEntity());
    mockMvc
        .perform(
            get("/api/v1/deliveries/report")
                .param("zoneId", plan.zoneId())
                .with(SignedInOrganization.register(mockMvc).auditor()))
        .andExpect(status().isNotFound());
  }

  @Test
  void theReportsOfAnEmergencyAreExportedForSinpad() throws Exception {
    SignedInOrganization organization = SignedInOrganization.register(mockMvc);
    String emergency = emergency(organization);
    String zone = zone(organization, emergency);
    structuredReport(organization, zone, "Reporte de Alto Perú", 35);

    byte[] csv =
        mockMvc
            .perform(
                get("/api/v1/emergencies/" + emergency + "/sinpad-export")
                    .with(organization.auditor()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/csv;charset=UTF-8"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    String[] lines = new String(csv, StandardCharsets.UTF_8).substring(1).split("\r\n");
    assertThat(lines).hasSize(2);
    assertThat(lines[0]).startsWith("EMERGENCIA,TIPO_FENOMENO,DEPARTAMENTO");
    assertThat(lines[1])
        .startsWith("Huaico Chosica 2026,Huaico o deslizamiento,Lima,Lima,Lurigancho-Chosica")
        .contains(",Sector Alto Perú,")
        .endsWith(",Grave,140,35,,,,Agua");
  }

  @Test
  void reportsThatLackSinpadFieldsAreListedAndNothingIsExported() throws Exception {
    SignedInOrganization organization = SignedInOrganization.register(mockMvc);
    String emergency = emergency(organization);
    String zone = zone(organization, emergency);
    structuredReport(organization, zone, "Reporte completo", 35);
    String incomplete = structuredReport(organization, zone, "Reporte sin familias", null);

    mockMvc
        .perform(
            get("/api/v1/emergencies/" + emergency + "/sinpad-export")
                .with(organization.auditor()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.incompleteReports[*].fieldReportId").value(contains(incomplete)))
        .andExpect(
            jsonPath("$.incompleteReports[0].missingFields").value(contains("familiesAffected")));
    mockMvc
        .perform(
            get("/api/v1/emergencies/" + emergency(organization) + "/sinpad-export")
                .with(organization.auditor()))
        .andExpect(status().isNotFound());
  }

  private String registerDelivery(ApprovedPlan plan) throws Exception {
    String delivery =
        JsonPath.read(
            mockMvc
                .perform(
                    get("/api/v1/deliveries")
                        .param("zoneId", plan.zoneId())
                        .with(plan.organization().auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
    String body =
        """
        {"deliveredAt": "2026-03-15T10:30:00",
         "items": [{"resourceId": "%s", "quantity": 120}, {"resourceId": "%s", "quantity": 80}]}
        """
            .formatted(plan.water(), plan.food());
    mockMvc
        .perform(
            multipart("/api/v1/deliveries/" + delivery + "/registration")
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
                .with(plan.organization().fieldBrigade()))
        .andExpect(status().isCreated());
    return delivery;
  }

  private String emergency(SignedInOrganization organization) throws Exception {
    return idOf(
        send(
            post("/api/v1/emergencies"),
            organization,
            """
            {"name": "Huaico Chosica 2026", "type": "LANDSLIDE",
             "location": {"department": "Lima", "province": "Lima",
                          "district": "Lurigancho-Chosica"},
             "startDate": "2026-03-14"}
            """));
  }

  private String zone(SignedInOrganization organization, String emergency) throws Exception {
    return idOf(
        send(
            post("/api/v1/emergencies/" + emergency + "/zones"),
            organization,
            """
            {"name": "Sector Alto Perú", "damageLevel": "SEVERE",
             "populationImpact": {"familiesAffected": 5, "peopleAffected": 20}}
            """));
  }

  private String structuredReport(
      SignedInOrganization organization, String zone, String text, Integer families)
      throws Exception {
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.SEVERE,
            families,
            140,
            null,
            null,
            null,
            Set.of(Need.WATER),
            families == null ? List.of("familiesAffected") : List.of()));
    return idOf(
        send(
            post("/api/v1/zones/" + zone + "/field-reports"),
            organization,
            "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text)));
  }

  private ResultActions send(
      MockHttpServletRequestBuilder request,
      SignedInOrganization organization,
      String body)
      throws Exception {
    return mockMvc.perform(
        request
            .with(organization.authority())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
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
}
