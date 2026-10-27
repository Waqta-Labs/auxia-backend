package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.spring.ScenarioScope;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * State of one scenario, shared by every step class: its emergency, its zones by name and the
 * last response of the API. A new instance is created for each scenario.
 */
@TestComponent
@ScenarioScope
public class EmergencyScenario {

  private final MockMvc mockMvc;
  private final Map<String, String> zoneIds = new HashMap<>();
  private String emergencyId;
  private MockHttpServletResponse response;

  public EmergencyScenario(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  public void registerEmergency(String name) throws Exception {
    emergencyId =
        idOf(
            send(
                post("/api/v1/emergencies"),
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
                    .formatted(name)));
  }

  /**
   * Registers a zone with 35 families and 140 people affected.
   *
   * @param population {@code null} for a zone whose population is unknown
   */
  public void registerZone(String name, Integer population, String damageLevel) throws Exception {
    String populationField = population == null ? "" : "\"population\": " + population + ", ";
    String zoneId =
        idOf(
            send(
                post("/api/v1/emergencies/" + emergencyId + "/zones"),
                """
                {
                  "name": "%s",
                  "populationImpact": { %s"familiesAffected": 35, "peopleAffected": 140 },
                  "damageLevel": "%s"
                }
                """
                    .formatted(name, populationField, damageLevel)));
    zoneIds.put(name, zoneId);
  }

  public MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception {
    response = mockMvc.perform(request).andReturn().getResponse();
    return response;
  }

  public MockHttpServletResponse send(MockHttpServletRequestBuilder request, String body)
      throws Exception {
    return send(request.contentType(MediaType.APPLICATION_JSON).content(body));
  }

  public String emergencyId() {
    return emergencyId;
  }

  public String zoneId(String name) {
    return zoneIds.get(name);
  }

  /** Name of the zone with that id, as it was registered in the scenario. */
  public String zoneName(String id) {
    return zoneIds.entrySet().stream()
        .filter(entry -> entry.getValue().equals(id))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow();
  }

  public MockHttpServletResponse response() {
    return response;
  }

  /** Reads a value of the last response with a JsonPath expression. */
  public <T> T read(String path) throws Exception {
    return JsonPath.read(response.getContentAsString(), path);
  }

  private static String idOf(MockHttpServletResponse created) throws Exception {
    assertThat(created.getStatus()).isEqualTo(201);
    return JsonPath.read(created.getContentAsString(), "$.id");
  }
}
