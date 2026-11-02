package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.DefaultAuthorityUserConfiguration;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DefaultAuthorityUserConfiguration.class})
@Transactional
class ZoneControllerTests {

  static final String ALTO_PERU_ZONE =
      """
      {
        "name": "Sector Alto Perú",
        "geolocation": { "latitude": -11.9436, "longitude": -76.7078 },
        "populationImpact": {
          "population": 600,
          "familiesAffected": 35,
          "peopleAffected": 140,
          "children": 40,
          "elderlyPeople": 15
        },
        "damageLevel": "SEVERE"
      }
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void registerReturnsCreatedWithTheRegisteredZone() throws Exception {
    String emergencyId = registerEmergency();

    mockMvc
        .perform(zonesOf(emergencyId).content(ALTO_PERU_ZONE))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/zones/")))
        .andExpect(jsonPath("$.emergencyId").value(emergencyId))
        .andExpect(jsonPath("$.name").value("Sector Alto Perú"))
        .andExpect(jsonPath("$.geolocation.latitude").value(-11.9436))
        .andExpect(jsonPath("$.populationImpact.population").value(600))
        .andExpect(jsonPath("$.populationImpact.peopleAffected").value(140))
        .andExpect(jsonPath("$.populationImpact.disabledPeople").value(0))
        .andExpect(jsonPath("$.damageLevel").value("SEVERE"))
        .andExpect(jsonPath("$.status").value("REGISTERED"));
  }

  @Test
  void geolocationAndPopulationAreOptional() throws Exception {
    String emergencyId = registerEmergency();

    mockMvc
        .perform(
            zonesOf(emergencyId)
                .content(
                    "{\"name\": \"Quebrada Pedregal\", \"populationImpact\": {},"
                        + " \"damageLevel\": \"LOW\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.geolocation").isEmpty())
        .andExpect(jsonPath("$.populationImpact.population").isEmpty())
        .andExpect(jsonPath("$.populationImpact.peopleAffected").value(0));
  }

  @Test
  void invalidZoneReturnsBadRequestWithTheInvalidFields() throws Exception {
    String emergencyId = registerEmergency();

    mockMvc
        .perform(
            zonesOf(emergencyId)
                .content(
                    "{\"name\": \"\", \"geolocation\": {\"latitude\": 95, \"longitude\": 0},"
                        + " \"populationImpact\": {\"children\": -1}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("geolocation.latitude")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("populationImpact.children")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("damageLevel")));
  }

  @Test
  void inconsistentPopulationReturnsUnprocessableContent() throws Exception {
    String emergencyId = registerEmergency();

    mockMvc
        .perform(
            zonesOf(emergencyId)
                .content(ALTO_PERU_ZONE.replace("\"children\": 40", "\"children\": 200")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.title").value("Business rule violated"))
        .andExpect(jsonPath("$.detail").value("children cannot be greater than peopleAffected."));
  }

  @Test
  void zoneOfAnUnknownEmergencyReturnsNotFound() throws Exception {
    mockMvc
        .perform(zonesOf(UUID.randomUUID().toString()).content(ALTO_PERU_ZONE))
        .andExpect(status().isNotFound());
  }

  @Test
  void zoneOfAClosedEmergencyReturnsConflict() throws Exception {
    String emergencyId = registerEmergency();
    mockMvc
        .perform(patch(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/close"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            zonesOf(emergencyId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es-PE")
                .content(ALTO_PERU_ZONE))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "La emergencia "
                        + emergencyId
                        + " está cerrada y no admite nuevas zonas."));
  }

  @Test
  void zonesOfAnEmergencyKeepTheirRegistrationOrder() throws Exception {
    String emergencyId = registerEmergency();
    String first = registerZone(emergencyId, ALTO_PERU_ZONE);
    String second =
        registerZone(
            emergencyId, ALTO_PERU_ZONE.replace("Sector Alto Perú", "Quebrada Pedregal"));

    mockMvc
        .perform(get(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/zones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(first, second)));
  }

  @Test
  void zonesOfAnUnknownEmergencyReturnNotFound() throws Exception {
    mockMvc
        .perform(get(EmergencyControllerTests.EMERGENCIES + "/" + UUID.randomUUID() + "/zones"))
        .andExpect(status().isNotFound());
  }

  @Test
  void findByIdReturnsTheZoneOrNotFound() throws Exception {
    String zoneId = registerZone(registerEmergency(), ALTO_PERU_ZONE);
    UUID unknown = UUID.randomUUID();

    mockMvc
        .perform(get("/api/v1/zones/" + zoneId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(zoneId));
    mockMvc
        .perform(get("/api/v1/zones/" + unknown))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Zone " + unknown + " does not exist."));
  }

  private MockHttpServletRequestBuilder zonesOf(String emergencyId) {
    return post(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/zones")
        .contentType(MediaType.APPLICATION_JSON);
  }

  private String registerEmergency() throws Exception {
    return idOf(
        mockMvc
            .perform(
                post(EmergencyControllerTests.EMERGENCIES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(EmergencyControllerTests.CHOSICA_EMERGENCY))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private String registerZone(String emergencyId, String zone) throws Exception {
    return idOf(
        mockMvc
            .perform(zonesOf(emergencyId).content(zone))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private static String idOf(String body) {
    return JsonPath.read(body, "$.id");
  }
}
