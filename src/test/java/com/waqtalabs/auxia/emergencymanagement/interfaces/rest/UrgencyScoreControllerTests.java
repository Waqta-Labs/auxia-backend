package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Urgency score, ranking and structuring through the API (US-02, US-04, US-05, US-06, TS-02).
 *
 * <p>Not transactional on purpose: the structuring and the score run after each commit, as in
 * production. Every test registers its own emergency, so the data of one test never reaches
 * another. The AI Service is {@link FakeAiService}: its score is {@code 40 × damage + 60 ×
 * peopleAffected / population}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeAiServiceConfiguration.class})
class UrgencyScoreControllerTests {

  private static final String REPORT = "Colapso total de viviendas, urge agua potable.";

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  @BeforeEach
  void resetAiService() {
    aiService.reset();
  }

  @Test
  void zoneWithPopulationIsRegisteredWithItsFirstScore() throws Exception {
    String emergencyId = registerEmergency();

    // 40 × 0.75 (SEVERE) + 60 × 140 / 600
    registerZone(emergencyId, "Sector Alto Perú", 600, "SEVERE")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PRIORITIZED"))
        .andExpect(jsonPath("$.score").value(44.0))
        .andExpect(jsonPath("$.priorityLevel").value("MEDIUM"));
  }

  @Test
  void scoreOfAZoneComesWithItsExplanation() throws Exception {
    String zoneId = idOf(registerZone(registerEmergency(), "Sector Alto Perú", 600, "SEVERE"));

    mockMvc
        .perform(get("/api/v1/zones/" + zoneId + "/score"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zoneId").value(zoneId))
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.score").value(44.0))
        .andExpect(jsonPath("$.priorityLevel").value("MEDIUM"))
        .andExpect(
            jsonPath("$.topFeatures[*].feature").value(contains("damageLevel", "peopleAffected")))
        .andExpect(jsonPath("$.topFeatures[0].contribution").value(30.0))
        .andExpect(jsonPath("$.modelVersion").value(FakeAiService.MODEL_VERSION))
        .andExpect(jsonPath("$.calculatedAt").value("2026-03-14T09:30:00"))
        .andExpect(jsonPath("$.missingVariables", empty()));
  }

  @Test
  void zoneWithoutPopulationHasItsScoreNotAvailable() throws Exception {
    String zoneId = idOf(registerZone(registerEmergency(), "Quebrada Pedregal", null, "LOW"));

    mockMvc
        .perform(get("/api/v1/zones/" + zoneId + "/score"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false))
        .andExpect(jsonPath("$.score").isEmpty())
        .andExpect(jsonPath("$.topFeatures", empty()))
        .andExpect(jsonPath("$.missingVariables").value(contains("population")));
    mockMvc
        .perform(post("/api/v1/zones/" + zoneId + "/score"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Zone "
                        + zoneId
                        + " has no population, so its urgency score is not available."));
  }

  @Test
  void reportIsRegisteredStructuredAndRaisesTheScoreOfItsZone() throws Exception {
    String zoneId = idOf(registerZone(registerEmergency(), "Sector Alto Perú", 600, "SEVERE"));
    aiService.willStructure(
        REPORT,
        new StructuredFieldData(
            DamageLevel.CRITICAL, null, 300, null, null, null, Set.of(Need.WATER), List.of()));

    registerReport(zoneId, REPORT)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.processed").value(true))
        .andExpect(jsonPath("$.structuredData.damageLevel").value("CRITICAL"))
        .andExpect(jsonPath("$.structuredData.peopleAffected").value(300))
        .andExpect(jsonPath("$.structuredData.needs").value(contains("WATER")));

    // 40 × 1 (CRITICAL from the report) + 60 × 300 / 600
    mockMvc
        .perform(get("/api/v1/zones/" + zoneId + "/score"))
        .andExpect(jsonPath("$.score").value(70.0))
        .andExpect(jsonPath("$.priorityLevel").value("HIGH"));
  }

  @Test
  void reportStaysNotProcessedWhileTheAiServiceDoesNotAnswer() throws Exception {
    String zoneId = idOf(registerZone(registerEmergency(), "Sector Alto Perú", 600, "SEVERE"));
    aiService.becomeUnavailable();

    String reportId =
        idOf(
            registerReport(zoneId, REPORT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processed").value(false))
                .andExpect(jsonPath("$.structuredData").isEmpty()));
    String structure = "/api/v1/field-reports/" + reportId + "/structure";

    mockMvc
        .perform(post(structure))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.title").value("Service unavailable"));

    aiService.becomeAvailable();
    mockMvc
        .perform(post(structure))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processed").value(true))
        .andExpect(jsonPath("$.structuredData.missingVariables").isNotEmpty());
    mockMvc
        .perform(post(structure))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value("Field report " + reportId + " is already structured."));
    mockMvc
        .perform(get("/api/v1/field-reports/" + reportId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processed").value(true));
  }

  @Test
  void scoreIsRecalculatedOnDemandWhenTheAiServiceAnswersAgain() throws Exception {
    String emergencyId = registerEmergency();
    aiService.becomeUnavailable();
    String zoneId = idOf(registerZone(emergencyId, "Sector Alto Perú", 600, "SEVERE"));
    String score = "/api/v1/zones/" + zoneId + "/score";

    mockMvc
        .perform(get(score))
        .andExpect(jsonPath("$.available").value(false))
        .andExpect(jsonPath("$.missingVariables", empty()));
    mockMvc.perform(post(score)).andExpect(status().isServiceUnavailable());

    aiService.becomeAvailable();
    mockMvc
        .perform(post(score))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.score").value(44.0));
  }

  @Test
  void assigningAPendingReportRecalculatesTheScoreOfTheZone() throws Exception {
    String emergencyId = registerEmergency();
    String zoneId = idOf(registerZone(emergencyId, "Sector Alto Perú", 600, "LOW"));
    String text = "Colapso total, hay 300 damnificados.";
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.CRITICAL, null, 300, null, null, null, Set.of(), List.of()));
    String reportId =
        idOf(
            mockMvc.perform(
                post("/api/v1/emergencies/" + emergencyId + "/field-reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\": \"" + text + "\", \"source\": \"VOLUNTEER\"}")));

    mockMvc
        .perform(
            patch("/api/v1/field-reports/" + reportId + "/zone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"zoneId\": \"" + zoneId + "\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/zones/" + zoneId + "/score"))
        .andExpect(jsonPath("$.score").value(70.0));
  }

  @Test
  void rankingGoesFromTheMostUrgentZoneAndFiltersByScoreAndNeed() throws Exception {
    String emergencyId = registerEmergency();
    String unscored = idOf(registerZone(emergencyId, "Sin población", null, "CRITICAL"));
    String moderate = idOf(registerZone(emergencyId, "Moderada", 600, "MODERATE")); // 34
    String critical = idOf(registerZone(emergencyId, "Crítica", 600, "CRITICAL")); // 54
    aiService.willStructure(
        REPORT,
        new StructuredFieldData(null, null, null, null, null, null, Set.of(Need.WATER), List.of()));
    registerReport(moderate, REPORT).andExpect(status().isCreated());
    String zones = "/api/v1/emergencies/" + emergencyId + "/zones";

    mockMvc
        .perform(get(zones))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(critical, moderate, unscored)))
        .andExpect(jsonPath("$[0].score").value(54.0))
        .andExpect(jsonPath("$[0].priorityLevel").value("HIGH"))
        .andExpect(jsonPath("$[2].score").isEmpty())
        .andExpect(jsonPath("$[1].needs").value(contains("WATER")));
    mockMvc
        .perform(get(zones).param("minScore", "50"))
        .andExpect(jsonPath("$[*].id").value(contains(critical)));
    mockMvc
        .perform(get(zones).param("need", "WATER"))
        .andExpect(jsonPath("$[*].id").value(contains(moderate)));
    mockMvc.perform(get(zones).param("maxScore", "101")).andExpect(status().isBadRequest());
  }

  @Test
  void unknownZoneOrReportReturnsNotFound() throws Exception {
    UUID unknown = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/zones/" + unknown + "/score")).andExpect(status().isNotFound());
    mockMvc.perform(post("/api/v1/zones/" + unknown + "/score")).andExpect(status().isNotFound());
    mockMvc
        .perform(post("/api/v1/field-reports/" + unknown + "/structure"))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/v1/field-reports/" + unknown)).andExpect(status().isNotFound());
  }

  private String registerEmergency() throws Exception {
    return idOf(
        mockMvc.perform(
            post(EmergencyControllerTests.EMERGENCIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(EmergencyControllerTests.CHOSICA_EMERGENCY)));
  }

  /** 140 people affected, so with a population of 600 they add 14 points. */
  private ResultActions registerZone(
      String emergencyId, String name, Integer population, String damageLevel) throws Exception {
    String populationField = population == null ? "" : "\"population\": " + population + ", ";
    return mockMvc.perform(
        post("/api/v1/emergencies/" + emergencyId + "/zones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "name": "%s",
                  "populationImpact": { %s"familiesAffected": 35, "peopleAffected": 140 },
                  "damageLevel": "%s"
                }
                """
                    .formatted(name, populationField, damageLevel)));
  }

  private ResultActions registerReport(String zoneId, String text) throws Exception {
    return mockMvc.perform(
        post("/api/v1/zones/" + zoneId + "/field-reports")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\": \"" + text + "\", \"source\": \"AUTHORITY\"}"));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }
}
