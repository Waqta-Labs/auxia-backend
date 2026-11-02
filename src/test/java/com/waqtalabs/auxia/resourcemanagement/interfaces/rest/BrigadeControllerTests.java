package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

import static com.waqtalabs.auxia.TestUsers.member;
import static com.waqtalabs.auxia.TestUsers.organizationAdmin;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Each test signs in as the ORGANIZATION_ADMIN of a new organization. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class BrigadeControllerTests {

  static final String BRIGADES = "/api/v1/brigades";
  static final String LEADER =
      "{\"fullName\": \"Rosa Quispe\", \"role\": \"LEADER\", \"phone\": \"987654321\"}";

  @Autowired private MockMvc mockMvc;

  private final UUID organizationId = UUID.randomUUID();
  private final RequestPostProcessor administrator = organizationAdmin(organizationId);

  @Test
  void createReturnsAnAvailableBrigadeOfTheOrganizationOfTheUser() throws Exception {
    create("Brigada Chosica 1")
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith(BRIGADES + "/")))
        .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
        .andExpect(jsonPath("$.name").value("Brigada Chosica 1"))
        .andExpect(jsonPath("$.baseLocation.latitude").value(-11.9436))
        .andExpect(jsonPath("$.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.hasLeader").value(false))
        .andExpect(jsonPath("$.members").isEmpty())
        .andExpect(jsonPath("$.currentAssignment").isEmpty());
  }

  @Test
  void repeatedNameInTheOrganizationReturnsConflict() throws Exception {
    create("Brigada Chosica 1").andExpect(status().isCreated());

    create("BRIGADA CHOSICA 1").andExpect(status().isConflict());
  }

  @Test
  void invalidBrigadeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            json(
                post(BRIGADES),
                "{\"name\": \"\", \"baseLocation\": {\"latitude\": 95, \"longitude\": 0}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("baseLocation.latitude")));
  }

  @Test
  void membersAreAddedAndRemoved() throws Exception {
    String brigadeId = idOf(create("Brigada Chosica 1"));

    String body =
        mockMvc
            .perform(json(post(members(brigadeId)), LEADER))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.hasLeader").value(true))
            .andExpect(jsonPath("$.members[0].fullName").value("Rosa Quispe"))
            .andExpect(jsonPath("$.members[0].role").value("LEADER"))
            .andExpect(jsonPath("$.members[0].phone").value("987654321"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String memberId = JsonPath.read(body, "$.members[0].id");

    mockMvc
        .perform(delete(members(brigadeId) + "/" + memberId).with(administrator))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members").isEmpty());
    mockMvc
        .perform(delete(members(brigadeId) + "/" + memberId).with(administrator))
        .andExpect(status().isNotFound());
  }

  @Test
  void availabilityChangesBetweenAvailableAndOffDuty() throws Exception {
    String brigadeId = idOf(create("Brigada Chosica 1"));

    changeAvailability(brigadeId, "OFF_DUTY")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OFF_DUTY"));
    changeAvailability(brigadeId, "ASSIGNED").andExpect(status().isUnprocessableContent());
    changeAvailability(brigadeId, "UNKNOWN").andExpect(status().isBadRequest());
  }

  @Test
  void brigadesOfTheOrganizationAreListedByNameAndStatus() throws Exception {
    String pedregal = idOf(create("Brigada Pedregal"));
    idOf(create("Brigada Alto Perú"));
    changeAvailability(pedregal, "OFF_DUTY").andExpect(status().isOk());

    mockMvc
        .perform(get(BRIGADES).with(administrator))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name").value(contains("Brigada Alto Perú", "Brigada Pedregal")));
    mockMvc
        .perform(get(BRIGADES).param("status", "OFF_DUTY").with(administrator))
        .andExpect(jsonPath("$[*].id").value(contains(pedregal)));
  }

  @Test
  void unknownBrigadeReturnsNotFound() throws Exception {
    UUID unknown = UUID.randomUUID();

    mockMvc
        .perform(get(BRIGADES + "/" + unknown).with(administrator))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Brigade " + unknown + " does not exist."));
    mockMvc
        .perform(json(post(members(unknown.toString())), LEADER))
        .andExpect(status().isNotFound());
  }

  @Test
  void brigadesOfAnotherOrganizationAreNeitherListedNorFound() throws Exception {
    String brigadeId = idOf(create("Brigada Chosica 1"));
    RequestPostProcessor otherAdministrator = organizationAdmin(UUID.randomUUID());

    mockMvc
        .perform(get(BRIGADES).with(otherAdministrator))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    mockMvc
        .perform(get(BRIGADES + "/" + brigadeId).with(otherAdministrator))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(json(post(members(brigadeId)), LEADER).with(otherAdministrator))
        .andExpect(status().isNotFound());
  }

  @Test
  void otherMembersOfTheOrganizationReadTheBrigadesButDoNotChangeThem() throws Exception {
    String brigadeId = idOf(create("Brigada Chosica 1"));
    RequestPostProcessor fieldBrigade = member(organizationId, "FIELD_BRIGADE");

    mockMvc.perform(get(BRIGADES + "/" + brigadeId).with(fieldBrigade)).andExpect(status().isOk());
    mockMvc
        .perform(json(post(members(brigadeId)), LEADER).with(fieldBrigade))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            json(patch(BRIGADES + "/" + brigadeId + "/availability"), "{\"status\": \"OFF_DUTY\"}")
                .with(member(organizationId, "AUTHORITY")))
        .andExpect(status().isForbidden());
  }

  private ResultActions create(String name) throws Exception {
    return mockMvc.perform(
        json(
            post(BRIGADES),
            """
            {
              "name": "%s",
              "baseLocation": { "latitude": -11.9436, "longitude": -76.7078 }
            }
            """
                .formatted(name)));
  }

  private ResultActions changeAvailability(String brigadeId, String status) throws Exception {
    return mockMvc.perform(
        json(
            patch(BRIGADES + "/" + brigadeId + "/availability"),
            "{\"status\": \"" + status + "\"}"));
  }

  private static String members(String brigadeId) {
    return BRIGADES + "/" + brigadeId + "/members";
  }

  /** A JSON request of the administrator; a later {@code with(...)} replaces the user. */
  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
    return request.with(administrator).contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }
}
