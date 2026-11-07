package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.FakeBlockchainConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Missions of the signed-in brigadist (A_07), through the account linked to a member. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  FakeBlockchainConfiguration.class
})
class MissionControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  @Test
  void brigadistLinkedToTheBrigadeSeesItsMissionWithTheZoneAndWhatToDeliver() throws Exception {
    aiService.reset();
    var plan =
        ApprovedPlan.prepareAt(mockMvc, aiService, "Sector Santa Rosa", -12.0432, -77.0428);
    var organization = plan.organization();
    String account = accountOf(organization);

    addMember(organization, plan.brigadeId(), account)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.members[*].userId").value(hasItem(account)));

    mockMvc
        .perform(get("/api/v1/me/missions").with(organization.fieldBrigade()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brigadeId").value(plan.brigadeId()))
        .andExpect(jsonPath("$.brigadeName").value("Brigada Chosica 1"))
        .andExpect(jsonPath("$.missions.length()").value(1))
        .andExpect(jsonPath("$.missions[0].status").value("ASSIGNED"))
        .andExpect(jsonPath("$.missions[0].deliveryId").value(notNullValue()))
        .andExpect(jsonPath("$.missions[0].planId").value(plan.planId()))
        .andExpect(jsonPath("$.missions[0].zoneId").value(plan.zoneId()))
        .andExpect(jsonPath("$.missions[0].zoneName").value("Sector Santa Rosa"))
        .andExpect(jsonPath("$.missions[0].category").value("SUPPLIES"))
        .andExpect(jsonPath("$.missions[0].score").isNumber())
        .andExpect(jsonPath("$.missions[0].reasons").isNotEmpty())
        .andExpect(jsonPath("$.missions[0].location.latitude").value(-12.0432))
        .andExpect(jsonPath("$.missions[0].distanceKm").isNumber())
        .andExpect(jsonPath("$.missions[0].etaMinutes").isNumber())
        .andExpect(jsonPath("$.missions[0].items[*].name").value(hasItem("Agua 2.5 L")));
  }

  @Test
  void accountIsLinkedOnceAndOnlyIfItIsABrigadistOfTheOrganization() throws Exception {
    aiService.reset();
    var plan = ApprovedPlan.prepare(mockMvc, aiService, "Sector " + UUID.randomUUID());
    var organization = plan.organization();
    String account = accountOf(organization);
    addMember(organization, plan.brigadeId(), account).andExpect(status().isCreated());

    addMember(organization, plan.brigadeId(), account).andExpect(status().isConflict());
    addMember(organization, plan.brigadeId(), UUID.randomUUID().toString())
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void accountOutsideEveryBrigadeHasNoMissionsAndOnlyBrigadistsAsk() throws Exception {
    var organization = SignedInOrganization.register(mockMvc);

    mockMvc
        .perform(get("/api/v1/me/missions").with(organization.fieldBrigade()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brigadeId").doesNotExist())
        .andExpect(jsonPath("$.missions").isEmpty());
    mockMvc
        .perform(get("/api/v1/me/missions").with(organization.administrator()))
        .andExpect(status().isForbidden());
  }

  private String accountOf(SignedInOrganization organization) throws Exception {
    return JsonPath.read(
        mockMvc
            .perform(get("/api/v1/users/me").with(organization.fieldBrigade()))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  private ResultActions addMember(
      SignedInOrganization organization, String brigadeId, String userId) throws Exception {
    return mockMvc.perform(
        post("/api/v1/brigades/" + brigadeId + "/members")
            .with(organization.administrator())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"fullName": "Luis Huamán", "role": "VOLUNTEER", "userId": "%s"}
                """
                    .formatted(userId)));
  }
}
