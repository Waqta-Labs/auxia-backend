package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/** Steps of US-01 and TS-01. Each scenario gets a new instance and its own emergency. */
public class FieldReportSteps {

  @Autowired private MockMvc mockMvc;

  private String emergencyId;
  private final Map<String, String> zoneIds = new HashMap<>();
  private MockHttpServletResponse response;

  @Dado("una emergencia activa {string} con la zona {string}")
  public void anActiveEmergencyWithAZone(String emergencyName, String zoneName) throws Exception {
    emergencyId =
        idOf(
            send(
                "/api/v1/emergencies",
                """
                {
                  "name": "%s",
                  "type": "LANDSLIDE",
                  "location": {
                    "department": "Lima",
                    "province": "Lima",
                    "district": "Lurigancho-Chosica"
                  },
                  "startDate": "2026-03-14"
                }
                """
                    .formatted(emergencyName)));
    String zoneId =
        idOf(
            send(
                "/api/v1/emergencies/" + emergencyId + "/zones",
                """
                {
                  "name": "%s",
                  "populationImpact": { "familiesAffected": 35, "peopleAffected": 140 },
                  "damageLevel": "SEVERE"
                }
                """
                    .formatted(zoneName)));
    zoneIds.put(zoneName, zoneId);
  }

  @Cuando("la autoridad registra el reporte {string} en la zona {string}")
  public void theAuthorityRegistersAReportInTheZone(String text, String zoneName)
      throws Exception {
    response = send(zoneReports(zoneName), report(text));
  }

  @Cuando("la autoridad registra en la emergencia el reporte {string} sin indicar la zona")
  public void theAuthorityRegistersAReportWithoutZone(String text) throws Exception {
    response = send("/api/v1/emergencies/" + emergencyId + "/field-reports", report(text));
  }

  @Cuando("se envía una solicitud POST con el texto de un reporte válido a la zona {string}")
  public void aPostWithAValidReportIsSent(String zoneName) throws Exception {
    response = send(zoneReports(zoneName), report("Derrumbe de viviendas, urge agua potable."));
  }

  @Cuando("se envía una solicitud POST sin el campo de texto del reporte a la zona {string}")
  public void aPostWithoutTextIsSent(String zoneName) throws Exception {
    response = send(zoneReports(zoneName), "{\"source\": \"AUTHORITY\"}");
  }

  @Entonces("el sistema guarda el reporte y lo asocia a la zona {string}")
  public void theReportIsStoredAndAssociatedToTheZone(String zoneName) throws Exception {
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(field("$.zoneId")).isEqualTo(zoneIds.get(zoneName));
    assertThat(reportIdsOf(zoneName)).contains(field("$.id"));
  }

  @Entonces("se marca el reporte como pendiente de asignación de zona")
  public void theReportIsPendingZoneAssignment() throws Exception {
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat((Boolean) JsonPath.read(response.getContentAsString(), "$.pendingZoneAssignment"))
        .isTrue();
    String pending =
        mockMvc
            .perform(
                get("/api/v1/emergencies/" + emergencyId + "/field-reports")
                    .param("pendingZoneAssignment", "true"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(JsonPath.<List<String>>read(pending, "$[*].id")).containsExactly(field("$.id"));
  }

  @Entonces("el endpoint responde con código {int} y el reporte registrado")
  public void theEndpointRespondsWithTheReport(int status) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(field("$.id")).isNotBlank();
    assertThat(field("$.text")).isEqualTo("Derrumbe de viviendas, urge agua potable.");
  }

  @Y("el reporte aparece al consultar los reportes de la zona {string}")
  public void theReportIsListedInTheZone(String zoneName) throws Exception {
    assertThat(reportIdsOf(zoneName)).contains(field("$.id"));
  }

  @Entonces("el endpoint responde con código {int} indicando el campo faltante {string}")
  public void theEndpointRespondsWithTheMissingField(int status, String fieldName)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<List<String>>read(response.getContentAsString(), "$.errors[*].field"))
        .containsExactly(fieldName);
  }

  private String zoneReports(String zoneName) {
    return "/api/v1/zones/" + zoneIds.get(zoneName) + "/field-reports";
  }

  private List<String> reportIdsOf(String zoneName) throws Exception {
    String body =
        mockMvc.perform(get(zoneReports(zoneName))).andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$[*].id");
  }

  private MockHttpServletResponse send(String path, String body) throws Exception {
    return mockMvc
        .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn()
        .getResponse();
  }

  private String field(String path) throws Exception {
    return JsonPath.read(response.getContentAsString(), path);
  }

  private static String report(String text) {
    return "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text);
  }

  private static String idOf(MockHttpServletResponse created) throws Exception {
    assertThat(created.getStatus()).isEqualTo(201);
    return JsonPath.read(created.getContentAsString(), "$.id");
  }
}
