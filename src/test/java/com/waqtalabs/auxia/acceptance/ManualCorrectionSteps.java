package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Steps of US-03 and US-07. The organization and its users sign in; the reports are structured by
 * the fake AI Service with the damage and people given.
 */
public class ManualCorrectionSteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  private final Map<String, String> zones = new HashMap<>();
  private SignedInOrganization organization;
  private String emergencyId;
  private String reportId;
  private String zoneId;
  private double scoreBefore;
  private MockHttpServletResponse response;

  @Before
  public void resetAiService() {
    aiService.reset();
  }

  @Dado(
      "un reporte estructurado de la zona {string} con daño {string} y {int} personas afectadas")
  public void aStructuredReport(String zone, String damage, int people) throws Exception {
    if (organization == null) {
      organization = SignedInOrganization.register(mockMvc);
      emergencyId =
          idOf(
              send(
                  post("/api/v1/emergencies"),
                  organization.authority(),
                  """
                  {"name": "Huaico Chosica 2026", "type": "LANDSLIDE",
                   "location": {"department": "Lima", "province": "Lima",
                                "district": "Lurigancho-Chosica"},
                   "startDate": "2026-03-14"}
                  """));
    }
    zoneId =
        idOf(
            send(
                post("/api/v1/emergencies/" + emergencyId + "/zones"),
                organization.authority(),
                """
                {"name": "%s", "damageLevel": "%s",
                 "populationImpact": {"population": 600, "familiesAffected": 5,
                                      "peopleAffected": 20}}
                """
                    .formatted(zone, damage)));
    zones.put(zone, zoneId);
    String text = "Reporte de " + zone + " " + zoneId;
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.valueOf(damage),
            null,
            people,
            null,
            null,
            null,
            Set.of(Need.WATER),
            List.of()));
    reportId =
        idOf(
            send(
                post("/api/v1/zones/" + zoneId + "/field-reports"),
                organization.authority(),
                "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text)));
    scoreBefore = score(zoneId);
  }

  @Cuando("la autoridad corrige las personas afectadas a {int} porque {string}")
  public void theAuthorityCorrects(int people, String reason) throws Exception {
    response =
        send(
                patch("/api/v1/field-reports/" + reportId + "/structured-data"),
                organization.authority(),
                "{\"peopleAffected\": %d, \"reason\": \"%s\"}".formatted(people, reason))
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Y("se consulta el reporte")
  public void theReportIsQueried() throws Exception {
    response =
        mockMvc
            .perform(get("/api/v1/field-reports/" + reportId).with(organization.auditor()))
            .andReturn()
            .getResponse();
  }

  @Entonces("el reporte usa {int} personas afectadas")
  public void theReportUses(int people) throws Exception {
    assertThat(
            JsonPath.<Integer>read(
                response.getContentAsString(), "$.structuredData.peopleAffected"))
        .isEqualTo(people);
  }

  @Y("el score de la zona se calcula con el valor corregido")
  public void theScoreUsesTheCorrection() throws Exception {
    assertThat(score(zoneId)).isGreaterThan(scoreBefore);
  }

  @Entonces(
      "se distingue el valor original {int} del valor corregido {int}, con la autoridad y el"
          + " motivo")
  public void originalAndCorrectedAreTold(int original, int corrected) throws Exception {
    String report = response.getContentAsString();
    assertThat(JsonPath.<Integer>read(report, "$.correction.originalData.peopleAffected"))
        .isEqualTo(original);
    assertThat(JsonPath.<Integer>read(report, "$.structuredData.peopleAffected"))
        .isEqualTo(corrected);
    assertThat(JsonPath.<String>read(report, "$.correction.correctedBy"))
        .isEqualTo(organization.authorityId().toString());
    assertThat(JsonPath.<String>read(report, "$.correction.reason")).isNotBlank();
  }

  @Cuando("la autoridad marca como prioritaria la zona {string} porque {string}")
  public void theAuthorityPrioritizes(String zone, String reason) throws Exception {
    response =
        send(
                put("/api/v1/zones/" + zones.get(zone) + "/manual-priority"),
                organization.authority(),
                "{\"reason\": \"%s\"}".formatted(reason))
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Entonces("la zona {string} registra la prioridad manual con la justificación y la autoridad")
  public void theZoneKeepsItsManualPriority(String zone) throws Exception {
    String body =
        mockMvc
            .perform(get("/api/v1/zones/" + zones.get(zone)).with(organization.auditor()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.manualPriority.reason")).isNotBlank();
    assertThat(JsonPath.<String>read(body, "$.manualPriority.setBy"))
        .isEqualTo(organization.authorityId().toString());
  }

  @Y("se consulta el listado de zonas")
  public void theRankingIsQueried() throws Exception {
    response =
        mockMvc
            .perform(
                get("/api/v1/emergencies/" + emergencyId + "/zones")
                    .with(organization.auditor()))
            .andReturn()
            .getResponse();
  }

  @Entonces("la zona {string} aparece primero marcada como prioridad manual")
  public void theZoneComesFirst(String zone) throws Exception {
    String ranking = response.getContentAsString();
    assertThat(JsonPath.<String>read(ranking, "$[0].id")).isEqualTo(zones.get(zone));
    assertThat(JsonPath.<Object>read(ranking, "$[0].manualPriority")).isNotNull();
  }

  @Y("la zona {string} aparece priorizada solo por su score")
  public void theZoneIsPrioritizedByItsScore(String zone) throws Exception {
    String ranking = response.getContentAsString();
    List<Map<String, Object>> items =
        JsonPath.read(ranking, "$[?(@.id == '" + zones.get(zone) + "')]");
    assertThat(items.getFirst().get("manualPriority")).isNull();
    assertThat(items.getFirst().get("score")).isNotNull();
  }

  private double score(String zone) throws Exception {
    Number score =
        JsonPath.read(
            mockMvc
                .perform(get("/api/v1/zones/" + zone).with(organization.auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.score");
    return score.doubleValue();
  }

  private MvcResult send(
      MockHttpServletRequestBuilder request, RequestPostProcessor user, String body)
      throws Exception {
    return mockMvc
        .perform(request.with(user).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn();
  }

  private static String idOf(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
