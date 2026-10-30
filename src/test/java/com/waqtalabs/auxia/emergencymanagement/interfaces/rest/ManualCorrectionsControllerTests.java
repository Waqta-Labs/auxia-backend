package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Manual corrections of an authority through the API: indicators of a report (US-03) and the
 * priority of a zone (US-07). Users sign in, because each one asks Identity Access for the role.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeAiServiceConfiguration.class})
class ManualCorrectionsControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  private SignedInOrganization organization;
  private String emergencyId;

  @BeforeEach
  void organizationWithAnEmergency() throws Exception {
    aiService.reset();
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

  @Test
  void correctedIndicatorsRecalculateTheScoreAndKeepTheOriginalOnes() throws Exception {
    String zoneId = zone("Sector Alto Perú", "MODERATE");
    String reportId = structuredReport(zoneId, DamageLevel.MODERATE, 60);
    double before = score(zoneId);

    send(
            patch("/api/v1/field-reports/" + reportId + "/structured-data"),
            organization.authority(),
            """
            {"damageLevel": "CRITICAL", "peopleAffected": 300, "reason": "Conteo en campo"}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.structuredData.damageLevel").value("CRITICAL"))
        .andExpect(jsonPath("$.structuredData.peopleAffected").value(300))
        .andExpect(jsonPath("$.structuredData.needs[0]").value("WATER"))
        .andExpect(jsonPath("$.correction.originalData.damageLevel").value("MODERATE"))
        .andExpect(jsonPath("$.correction.originalData.peopleAffected").value(60))
        .andExpect(
            jsonPath("$.correction.correctedBy").value(organization.authorityId().toString()))
        .andExpect(jsonPath("$.correction.reason").value("Conteo en campo"));

    assertThat(score(zoneId)).isGreaterThan(before);
  }

  @Test
  void correctionNeedsAStructuredReportAReasonAndAnAuthority() throws Exception {
    String zoneId = zone("Sector Alto Perú", "MODERATE");
    String reportId = structuredReport(zoneId, DamageLevel.MODERATE, 60);
    String body = "{\"peopleAffected\": 80, \"reason\": \"Conteo\"}";

    send(
            patch("/api/v1/field-reports/" + reportId + "/structured-data"),
            organization.authority(),
            "{\"peopleAffected\": 80}")
        .andExpect(status().isBadRequest());
    send(
            patch("/api/v1/field-reports/" + reportId + "/structured-data"),
            organization.auditor(),
            body)
        .andExpect(status().isForbidden());
    aiService.becomeUnavailable();
    String pending = idOf(report(zoneId, "Otro reporte"));
    send(
            patch("/api/v1/field-reports/" + pending + "/structured-data"),
            organization.authority(),
            body)
        .andExpect(status().isConflict());
  }

  @Test
  void zonePrioritizedByHandGoesFirstAndShowsWhoAndWhy() throws Exception {
    String urgent = zone("Sector Alto Perú", "CRITICAL");
    structuredReport(urgent, DamageLevel.CRITICAL, 300);
    String quiet = zone("Quebrada Pedregal", "LOW");
    structuredReport(quiet, DamageLevel.LOW, 10);

    send(
            put("/api/v1/zones/" + quiet + "/manual-priority"),
            organization.authority(),
            "{\"reason\": \"Riesgo inminente de un nuevo huaico\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.manualPriority.reason").value("Riesgo inminente de un nuevo huaico"))
        .andExpect(jsonPath("$.manualPriority.setBy").value(organization.authorityId().toString()));

    ranking()
        .andExpect(jsonPath("$[0].id").value(quiet))
        .andExpect(jsonPath("$[0].manualPriority.reason").exists())
        .andExpect(jsonPath("$[1].id").value(urgent))
        .andExpect(jsonPath("$[1].manualPriority").doesNotExist());

    mockMvc
        .perform(
            delete("/api/v1/zones/" + quiet + "/manual-priority").with(organization.authority()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.manualPriority").doesNotExist());
    ranking().andExpect(jsonPath("$[0].id").value(urgent));
  }

  @Test
  void manualPriorityNeedsAJustificationAndAnAuthority() throws Exception {
    String zoneId = zone("Sector Alto Perú", "SEVERE");

    send(put("/api/v1/zones/" + zoneId + "/manual-priority"), organization.authority(), "{}")
        .andExpect(status().isBadRequest());
    send(
            put("/api/v1/zones/" + zoneId + "/manual-priority"),
            organization.auditor(),
            "{\"reason\": \"Riesgo\"}")
        .andExpect(status().isForbidden());
  }

  private String zone(String name, String damage) throws Exception {
    return idOf(
        send(
            post("/api/v1/emergencies/" + emergencyId + "/zones"),
            organization.authority(),
            """
            {"name": "%s", "damageLevel": "%s",
             "populationImpact": {"population": 600, "familiesAffected": 10,
                                  "peopleAffected": 40}}
            """
                .formatted(name, damage)));
  }

  private String structuredReport(String zoneId, DamageLevel damage, int people) throws Exception {
    String text = "Reporte " + zoneId + " " + damage;
    aiService.willStructure(
        text,
        new StructuredFieldData(
            damage, null, people, null, null, null, Set.of(Need.WATER), List.of()));
    return idOf(report(zoneId, text).andExpect(jsonPath("$.processed").value(true)));
  }

  private ResultActions report(String zoneId, String text) throws Exception {
    return send(
        post("/api/v1/zones/" + zoneId + "/field-reports"),
        organization.authority(),
        "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text));
  }

  private double score(String zoneId) throws Exception {
    Number score =
        JsonPath.read(
            mockMvc
                .perform(get("/api/v1/zones/" + zoneId).with(organization.auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.score");
    return score.doubleValue();
  }

  private ResultActions ranking() throws Exception {
    return mockMvc.perform(
        get("/api/v1/emergencies/" + emergencyId + "/zones").with(organization.auditor()));
  }

  private ResultActions send(
      MockHttpServletRequestBuilder request, RequestPostProcessor user, String body)
      throws Exception {
    return mockMvc.perform(
        request.with(user).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }
}
