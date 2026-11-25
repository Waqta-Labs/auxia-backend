package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.ApprovedPlan.ProposedPlan;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Steps of the SOS that citizens send from the mobile app, added to US-24 and US-06 in Sprint 2
 * after the validation interviews (section 7.3 of the report). The scenarios commit, so each zone
 * is placed far from the zones of the other scenarios and each SOS comes from its own address.
 */
public class CitizenSosSteps {

  private static final String SOS_PATH = "/api/v1/public/field-reports";
  private static final AtomicInteger SPOTS = new AtomicInteger();
  private static final AtomicInteger ADDRESSES = new AtomicInteger();

  private final MockMvc mockMvc;
  private final FakeAiService aiService;
  private final EmergencyScenario scenario;

  private ProposedPlan plan;
  private String sosId;

  public CitizenSosSteps(MockMvc mockMvc, FakeAiService aiService, EmergencyScenario scenario) {
    this.mockMvc = mockMvc;
    this.aiService = aiService;
    this.scenario = scenario;
  }

  @Before
  public void resetAiService() {
    aiService.reset();
  }

  @Dado("que un ciudadano envió un SOS desde la zona {string}")
  public void aCitizenSentAnSosFromTheZone(String zone) throws Exception {
    double[] spot = nextSpot();
    plan = ApprovedPlan.proposeAt(mockMvc, aiService, zone, spot[0], spot[1], false);
    sosId = sendSos(spot[0] - 0.009, spot[1], List.of("ROAD_BLOCKED"));
  }

  @Cuando("una autoridad aprueba el plan de ayuda de la zona y no hay ninguna brigada libre")
  public void anAuthorityApprovesThePlanWithoutAFreeBrigade() throws Exception {
    plan.approve();
  }

  @Entonces("el SOS muestra la etapa {string} con la hora de la aprobación")
  public void theSosShowsTheStageWithTheTimeOfTheApproval(String stage) throws Exception {
    MockHttpServletResponse response =
        mockMvc.perform(get(SOS_PATH + "/" + sosId)).andReturn().getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    String json = response.getContentAsString();
    assertThat(JsonPath.<String>read(json, "$.status")).isEqualTo(stage);
    assertThat(JsonPath.<String>read(json, "$.acceptedAt")).isNotBlank();
    assertThat(JsonPath.<Object>read(json, "$.etaMinutes")).isNull();
  }

  @Dado("que {int} ciudadanos reportan la vía bloqueada en la zona {string}")
  public void citizensReportABlockedRoadInTheZone(int citizens, String zone) throws Exception {
    double[] spot = nextSpot();
    scenario.send(
        post("/api/v1/emergencies/" + scenario.emergencyId() + "/zones"),
        """
        {
          "name": "%s",
          "geolocation": { "latitude": %s, "longitude": %s },
          "populationImpact": { "population": 600, "familiesAffected": 35, "peopleAffected": 140 },
          "damageLevel": "SEVERE"
        }
        """
            .formatted(zone, spot[0], spot[1]));
    assertThat(scenario.response().getStatus()).isEqualTo(201);
    for (int sent = 0; sent < citizens; sent++) {
      sendSos(spot[0] - 0.009, spot[1], List.of("ROAD_BLOCKED", "NO_DRINKING_WATER"));
    }
    sendSos(spot[0] - 0.009, spot[1], List.of());
  }

  @Entonces("la zona {string} muestra {int} reportes de {string}")
  public void theZoneShowsReportsOf(String zone, int reports, String condition) throws Exception {
    List<Map<String, Object>> zones = scenario.read("$[?(@.name == '" + zone + "')]");
    assertThat(zones).hasSize(1);
    @SuppressWarnings("unchecked")
    Map<String, Integer> conditions = (Map<String, Integer>) zones.getFirst().get("accessConditions");
    assertThat(conditions).containsEntry(condition, reports);
  }

  /** Somewhere in the South Pacific, far from the spot of any other test. */
  private static double[] nextSpot() {
    return new double[] {-48, -170 + 2.5 * SPOTS.getAndIncrement()};
  }

  /** An address that sent no other SOS, so the rate limit of each sender never applies. */
  private static String address() {
    int next = ADDRESSES.getAndIncrement();
    return "198.51." + (100 + next / 250) + "." + (next % 250 + 1);
  }

  /** Sends the SOS as the mobile app does, from an address of its own. */
  private String sendSos(double latitude, double longitude, List<String> accessConditions)
      throws Exception {
    String id = UUID.randomUUID().toString();
    String conditions =
        String.join(", ", accessConditions.stream().map(c -> "\"" + c + "\"").toList());
    String reportedAt =
        LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString();
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post(SOS_PATH)
                    .header("X-Forwarded-For", address())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "clientReportId": "%s",
                          "source": "CITIZEN",
                          "category": "RESCUE",
                          "text": "SOS Rescate. Necesitan ayuda 2 adultos mayores.",
                          "reportedAt": "%s",
                          "location": { "latitude": %s, "longitude": %s },
                          "people": { "children": 0, "elderly": 2, "injured": 0 },
                          "accessConditions": [%s]
                        }
                        """
                            .formatted(id, reportedAt, latitude, longitude, conditions)))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(201);
    return id;
  }
}
