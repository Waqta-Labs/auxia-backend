package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.List;

/** Steps of US-01, US-02 and TS-01. */
public class FieldReportSteps {

  private final EmergencyScenario scenario;

  public FieldReportSteps(EmergencyScenario scenario) {
    this.scenario = scenario;
  }

  @Dado("una emergencia activa {string} con la zona {string}")
  public void anActiveEmergencyWithAZone(String emergencyName, String zoneName) throws Exception {
    scenario.registerEmergency(emergencyName);
    scenario.registerZone(zoneName, null, "SEVERE");
  }

  @Cuando("la autoridad registra el reporte {string} en la zona {string}")
  public void theAuthorityRegistersAReportInTheZone(String text, String zoneName)
      throws Exception {
    scenario.send(post(zoneReports(zoneName)), report(text));
  }

  @Cuando("la autoridad registra en la emergencia el reporte {string} sin indicar la zona")
  public void theAuthorityRegistersAReportWithoutZone(String text) throws Exception {
    scenario.send(
        post("/api/v1/emergencies/" + scenario.emergencyId() + "/field-reports"), report(text));
  }

  @Cuando("se envía una solicitud POST con el texto de un reporte válido a la zona {string}")
  public void aPostWithAValidReportIsSent(String zoneName) throws Exception {
    scenario.send(
        post(zoneReports(zoneName)), report("Derrumbe de viviendas, urge agua potable."));
  }

  @Cuando("se envía una solicitud POST sin el campo de texto del reporte a la zona {string}")
  public void aPostWithoutTextIsSent(String zoneName) throws Exception {
    scenario.send(post(zoneReports(zoneName)), "{\"source\": \"AUTHORITY\"}");
  }

  @Entonces("el sistema guarda el reporte y lo asocia a la zona {string}")
  public void theReportIsStoredAndAssociatedToTheZone(String zoneName) throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(201);
    assertThat(scenario.<String>read("$.zoneId")).isEqualTo(scenario.zoneId(zoneName));
    String reportId = scenario.read("$.id");
    assertThat(reportIdsOf(zoneName)).contains(reportId);
  }

  @Entonces("se marca el reporte como pendiente de asignación de zona")
  public void theReportIsPendingZoneAssignment() throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(201);
    assertThat(scenario.<Boolean>read("$.pendingZoneAssignment")).isTrue();
    String reportId = scenario.read("$.id");
    String pending =
        scenario
            .send(
                get("/api/v1/emergencies/" + scenario.emergencyId() + "/field-reports")
                    .param("pendingZoneAssignment", "true"))
            .getContentAsString();
    assertThat(JsonPath.<List<String>>read(pending, "$[*].id")).containsExactly(reportId);
  }

  @Entonces("el endpoint responde con código {int} y el reporte registrado")
  public void theEndpointRespondsWithTheReport(int status) throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(status);
    assertThat(scenario.<String>read("$.id")).isNotBlank();
    assertThat(scenario.<String>read("$.text"))
        .isEqualTo("Derrumbe de viviendas, urge agua potable.");
  }

  @Y("el reporte aparece al consultar los reportes de la zona {string}")
  public void theReportIsListedInTheZone(String zoneName) throws Exception {
    String reportId = scenario.read("$.id");
    assertThat(reportIdsOf(zoneName)).contains(reportId);
  }

  @Entonces("el endpoint responde con código {int} indicando el campo faltante {string}")
  public void theEndpointRespondsWithTheMissingField(int status, String fieldName)
      throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(status);
    assertThat(scenario.<List<String>>read("$.errors[*].field")).containsExactly(fieldName);
  }

  @Entonces("el reporte queda procesado con el daño {string}, {int} personas afectadas y la"
      + " necesidad {string}")
  public void theReportIsProcessedWith(String damageLevel, int peopleAffected, String need)
      throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(201);
    assertThat(scenario.<Boolean>read("$.processed")).isTrue();
    assertThat(scenario.<String>read("$.structuredData.damageLevel")).isEqualTo(damageLevel);
    assertThat(scenario.<Integer>read("$.structuredData.peopleAffected"))
        .isEqualTo(peopleAffected);
    assertThat(scenario.<List<String>>read("$.structuredData.needs")).containsExactly(need);
  }

  @Entonces("el reporte queda procesado con {string} y {string} marcadas como no disponibles")
  public void theReportIsProcessedWithMissingVariables(String first, String second)
      throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(201);
    assertThat(scenario.<Boolean>read("$.processed")).isTrue();
    assertThat(scenario.<Object>read("$.structuredData.damageLevel")).isNull();
    assertThat(scenario.<List<String>>read("$.structuredData.missingVariables"))
        .contains(first, second);
  }

  private String zoneReports(String zoneName) {
    return "/api/v1/zones/" + scenario.zoneId(zoneName) + "/field-reports";
  }

  private List<String> reportIdsOf(String zoneName) throws Exception {
    String body = scenario.send(get(zoneReports(zoneName))).getContentAsString();
    return JsonPath.read(body, "$[*].id");
  }

  private static String report(String text) {
    return "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text);
  }
}
