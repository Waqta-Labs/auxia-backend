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
class FieldReportControllerTests {

  private static final String REPORT =
      """
      { "text": "Derrumbe de viviendas. Hay 35 familias afectadas.", "source": "AUTHORITY" }
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void reportOfAZoneReturnsCreatedAndBelongsToTheZone() throws Exception {
    String emergencyId = registerEmergency();
    String zoneId = registerZone(emergencyId, "Sector Alto Perú");

    mockMvc
        .perform(json(post("/api/v1/zones/" + zoneId + "/field-reports")).content(REPORT))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/field-reports/")))
        .andExpect(jsonPath("$.emergencyId").value(emergencyId))
        .andExpect(jsonPath("$.zoneId").value(zoneId))
        .andExpect(jsonPath("$.text").value("Derrumbe de viviendas. Hay 35 familias afectadas."))
        .andExpect(jsonPath("$.source").value("AUTHORITY"))
        .andExpect(jsonPath("$.reportedAt").isNotEmpty())
        .andExpect(jsonPath("$.pendingZoneAssignment").value(false));
  }

  @Test
  void reportWithoutTextReturnsBadRequestNamingTheMissingField() throws Exception {
    String zoneId = registerZone(registerEmergency(), "Sector Alto Perú");

    mockMvc
        .perform(
            json(post("/api/v1/zones/" + zoneId + "/field-reports"))
                .content("{\"source\": \"CITIZEN\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(contains("text")));
  }

  @Test
  void reportOfAnUnknownZoneReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            json(post("/api/v1/zones/" + UUID.randomUUID() + "/field-reports")).content(REPORT))
        .andExpect(status().isNotFound());
  }

  @Test
  void reportOfAnEmergencyIsAssociatedToTheZoneMentionedInTheText() throws Exception {
    String emergencyId = registerEmergency();
    String zoneId = registerZone(emergencyId, "Sector Alto Perú");

    mockMvc
        .perform(
            reportsOf(emergencyId)
                .content(
                    "{\"text\": \"Derrumbe en el sector alto peru\", \"source\": \"VOLUNTEER\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.zoneId").value(zoneId))
        .andExpect(jsonPath("$.pendingZoneAssignment").value(false));
  }

  @Test
  void pendingReportCanBeListedAndThenAssigned() throws Exception {
    String emergencyId = registerEmergency();
    String zoneId = registerZone(emergencyId, "Sector Alto Perú");
    String reportId =
        idOf(
            mockMvc
                .perform(reportsOf(emergencyId).content(REPORT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zoneId").isEmpty())
                .andExpect(jsonPath("$.pendingZoneAssignment").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString());

    mockMvc
        .perform(
            get(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/field-reports")
                .param("pendingZoneAssignment", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(reportId)));

    mockMvc
        .perform(
            json(patch("/api/v1/field-reports/" + reportId + "/zone"))
                .content("{\"zoneId\": \"" + zoneId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zoneId").value(zoneId))
        .andExpect(jsonPath("$.pendingZoneAssignment").value(false));

    mockMvc
        .perform(get("/api/v1/zones/" + zoneId + "/field-reports"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(hasItem(reportId)));
  }

  @Test
  void assigningTwiceReturnsConflict() throws Exception {
    String emergencyId = registerEmergency();
    String zoneId = registerZone(emergencyId, "Sector Alto Perú");
    String reportId = registerZoneReport(zoneId);

    mockMvc
        .perform(
            json(patch("/api/v1/field-reports/" + reportId + "/zone"))
                .content("{\"zoneId\": \"" + zoneId + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Field report " + reportId + " already has a zone."));
  }

  @Test
  void zoneOfAnotherEmergencyReturnsUnprocessableContent() throws Exception {
    String emergencyId = registerEmergency();
    String otherZoneId = registerZone(registerEmergency(), "Castilla");

    mockMvc
        .perform(
            reportsOf(emergencyId)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es-PE")
                .content(
                    "{\"text\": \"Calles inundadas\", \"source\": \"CITIZEN\", \"zoneId\": \""
                        + otherZoneId
                        + "\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "La zona "
                        + otherZoneId
                        + " no pertenece a la emergencia "
                        + emergencyId
                        + "."));
  }

  @Test
  void reportsOfAnEmergencyIncludeAssignedAndPendingOnes() throws Exception {
    String emergencyId = registerEmergency();
    String assigned = registerZoneReport(registerZone(emergencyId, "Sector Alto Perú"));
    String pending =
        idOf(
            mockMvc
                .perform(reportsOf(emergencyId).content(REPORT))
                .andReturn()
                .getResponse()
                .getContentAsString());

    mockMvc
        .perform(get(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/field-reports"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(hasItem(assigned)))
        .andExpect(jsonPath("$[*].id").value(hasItem(pending)));
  }

  @Test
  void reportsOfUnknownZonesAndEmergenciesReturnNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/zones/" + UUID.randomUUID() + "/field-reports"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get(EmergencyControllerTests.EMERGENCIES + "/" + UUID.randomUUID() + "/field-reports"))
        .andExpect(status().isNotFound());
  }

  private MockHttpServletRequestBuilder reportsOf(String emergencyId) {
    return json(post(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/field-reports"));
  }

  private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request) {
    return request.contentType(MediaType.APPLICATION_JSON);
  }

  private String registerEmergency() throws Exception {
    return created(
        json(post(EmergencyControllerTests.EMERGENCIES))
            .content(EmergencyControllerTests.CHOSICA_EMERGENCY));
  }

  private String registerZone(String emergencyId, String name) throws Exception {
    return created(
        json(post(EmergencyControllerTests.EMERGENCIES + "/" + emergencyId + "/zones"))
            .content(ZoneControllerTests.ALTO_PERU_ZONE.replace("Sector Alto Perú", name)));
  }

  private String registerZoneReport(String zoneId) throws Exception {
    return created(json(post("/api/v1/zones/" + zoneId + "/field-reports")).content(REPORT));
  }

  private String created(MockHttpServletRequestBuilder request) throws Exception {
    return idOf(
        mockMvc
            .perform(request)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private static String idOf(String body) {
    return JsonPath.read(body, "$.id");
  }
}
