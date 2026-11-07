package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The SOS of the mobile app, without signing in. The tests commit, so each one places its zone
 * far from the zones of the others, and sends from an address of its own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeAiServiceConfiguration.class})
class CitizenReportControllerTests {

  private static final String SOS_PATH = "/api/v1/public/field-reports";
  private static final String TEXT = "SOS Médico. Necesitan ayuda 3 niños y 2 heridos.";
  private static final AtomicInteger SPOTS = new AtomicInteger();

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  private SignedInOrganization organization;
  private final String address = "190.12.%d.%d".formatted(SPOTS.get() % 250, SPOTS.get() / 250);

  /** Somewhere in the South Pacific, more than 200 km from the spot of any other test. */
  private final double latitude = -45;
  private final double longitude = -170 + 2.5 * SPOTS.getAndIncrement();

  @BeforeEach
  void signIn() throws Exception {
    aiService.reset();
    organization = SignedInOrganization.register(mockMvc);
  }

  @Test
  void sosWithoutSignInIsCreatedAndItsRetryReturnsItWithoutStoringItTwice() throws Exception {
    String emergencyId = emergency();
    zone(emergencyId, "Sector Alto Perú", null);
    String id = UUID.randomUUID().toString();

    send(sos(id, latitude + 0.01, longitude))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, SOS_PATH + "/" + id))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.score").value(nullValue()))
        .andExpect(jsonPath("$.etaMinutes").value(nullValue()));
    send(sos(id, latitude + 0.01, longitude))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECEIVED"));

    mockMvc
        .perform(
            get("/api/v1/emergencies/" + emergencyId + "/field-reports")
                .with(organization.authority()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].source").value("CITIZEN"))
        .andExpect(jsonPath("$[0].text").value(TEXT))
        .andExpect(jsonPath("$[0].pendingZoneAssignment").value(false));
  }

  @Test
  void progressOfAnSosIsPublicAndNeverCached() throws Exception {
    zone(emergency(), "Sector Alto Perú", null);
    String id = UUID.randomUUID().toString();
    send(sos(id, latitude, longitude)).andExpect(status().isCreated());

    mockMvc
        .perform(get(SOS_PATH + "/" + id))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, not("max-age=60, public")))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void sosNearAZoneWithScoreIsPrioritizedWithoutShowingTheScore() throws Exception {
    zone(emergency(), "Quebrada Pedregal", 600);
    String id = UUID.randomUUID().toString();
    send(sos(id, latitude, longitude + 0.01)).andExpect(status().isCreated());

    mockMvc
        .perform(get(SOS_PATH + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PRIORITIZED"))
        .andExpect(jsonPath("$.score").value(nullValue()));
  }

  @Test
  void sosNearAZoneWhoseApprovedPlanABrigadeTookHasTheBrigadeOnTheWay() throws Exception {
    ApprovedPlan.prepareAt(mockMvc, aiService, "Sector Nicolás de Piérola", latitude, longitude);
    String id = UUID.randomUUID().toString();
    send(sos(id, latitude - 0.01, longitude)).andExpect(status().isCreated());

    mockMvc
        .perform(get(SOS_PATH + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BRIGADE_ON_THE_WAY"))
        .andExpect(jsonPath("$.score").value(nullValue()))
        .andExpect(jsonPath("$.etaMinutes").value(nullValue()));
  }

  @Test
  void invalidSosIsRejectedNamingTheInvalidFields() throws Exception {
    send(
            """
            {
              "clientReportId": null,
              "category": "MEDICAL",
              "text": "%s",
              "reportedAt": "2026-11-06T14:41:05",
              "location": { "latitude": -91, "longitude": -76.69 },
              "people": { "children": 501, "elderly": -1, "injured": 0 },
              "accessConditions": ["ROAD_BLOCKED", "NO_DRINKING_WATER", "NO_CELL_SIGNAL",
                                   "DAMAGED_BUILDING", "ROAD_BLOCKED"]
            }
            """
                .formatted("a".repeat(2001)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    containsInAnyOrder(
                        "clientReportId",
                        "text",
                        "location.latitude",
                        "people.children",
                        "people.elderly",
                        "accessConditions")));
  }

  @Test
  void unknownCategoryOrMalformedIdIsRejected() throws Exception {
    String id = UUID.randomUUID().toString();
    send(sos(id, latitude, longitude).replace("\"MEDICAL\"", "\"FIRE\""))
        .andExpect(status().isBadRequest());
    send(sos("3f0c", latitude, longitude)).andExpect(status().isBadRequest());
  }

  @Test
  void unknownSosIsNotFound() throws Exception {
    mockMvc
        .perform(get(SOS_PATH + "/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void addressOverTheLimitWaitsButItsRetriesAreStillAnswered() throws Exception {
    zone(emergency(), "Sector Alto Perú", null);
    String first = UUID.randomUUID().toString();
    send(sos(first, latitude, longitude)).andExpect(status().isCreated());
    for (int sent = 1; sent < 10; sent++) {
      send(sos(UUID.randomUUID().toString(), latitude, longitude))
          .andExpect(status().isCreated());
    }

    send(sos(UUID.randomUUID().toString(), latitude, longitude))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(429));
    send(sos(first, latitude, longitude)).andExpect(status().isOk());
    mockMvc
        .perform(
            post(SOS_PATH)
                .header("X-Forwarded-For", "181.65.2.33:41022")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sos(UUID.randomUUID().toString(), latitude, longitude)))
        .andExpect(status().isCreated());
  }

  private ResultActions send(String body) throws Exception {
    return mockMvc.perform(
        post(SOS_PATH)
            .header("X-Forwarded-For", "1.1.1.1, " + address + ":52814")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static String sos(String id, double latitude, double longitude) {
    return """
        {
          "clientReportId": "%s",
          "source": "CITIZEN",
          "category": "MEDICAL",
          "text": "%s",
          "reportedAt": "2026-11-06T14:41:05",
          "location": { "latitude": %s, "longitude": %s },
          "people": { "children": 3, "elderly": 0, "injured": 2 },
          "accessConditions": ["ROAD_BLOCKED"]
        }
        """
        .formatted(id, TEXT, latitude, longitude);
  }

  private String emergency() throws Exception {
    return idOf(
        mockMvc.perform(
            post("/api/v1/emergencies")
                .with(organization.authority())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Huaico Chosica 2026",
                      "type": "LANDSLIDE",
                      "location": { "department": "Lima", "province": "Lima",
                                    "district": "Lurigancho-Chosica" },
                      "startDate": "2026-03-14"
                    }
                    """)));
  }

  /**
   * A zone at the spot of the test.
   *
   * @param population {@code null} for a zone without score
   */
  private String zone(String emergencyId, String name, Integer population) throws Exception {
    return idOf(
        mockMvc.perform(
            post("/api/v1/emergencies/" + emergencyId + "/zones")
                .with(organization.authority())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "%s",
                      "geolocation": { "latitude": %s, "longitude": %s },
                      "populationImpact": { "population": %s, "familiesAffected": 75,
                                            "peopleAffected": 300 },
                      "damageLevel": "CRITICAL"
                    }
                    """
                        .formatted(name, latitude, longitude, population))));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(
        result
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }
}
