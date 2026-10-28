package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Each test registers its own organization, inventory and emergency. The class is not
 * transactional: structuring a report and scoring its zone run after the commit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeAiServiceConfiguration.class})
class DistributionPlanControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  private SignedInOrganization organization;
  private String water;
  private String food;
  private String emergencyId;
  private String altoPeru;
  private String pedregal;

  /**
   * An inventory of 500 units of water and 300 of food, and an emergency where Alto Perú needs
   * water and food and Pedregal, less urgent, needs water. The fake recommendation gives each
   * resource to the most urgent zone that needs it, so everything goes to Alto Perú.
   */
  @BeforeEach
  void organizationWithInventoryAndAnEmergency() throws Exception {
    aiService.reset();
    organization = SignedInOrganization.register(mockMvc);
    String inventoryId =
        idOf(
            send(
                post("/api/v1/inventories"),
                organization.administrator(),
                "{\"name\": \"Lima\"}"));
    water = resource(inventoryId, "Agua 2.5 L", "WATER", 500);
    food = resource(inventoryId, "Kit de alimentos", "FOOD", 300);
    emergencyId = registerEmergency(organization);
    altoPeru =
        scoredZone(
            organization, emergencyId, "Sector Alto Perú", "CRITICAL", Need.WATER, Need.FOOD);
    pedregal =
        scoredZone(organization, emergencyId, "Quebrada Pedregal", "MODERATE", Need.WATER);
  }

  @Test
  void recommendationProposesAndReservesThePlansAndListsTheUnattendedZones() throws Exception {
    String castilla = zone(organization, emergencyId, "Castilla", "LOW");

    recommend(organization.authority())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.plans.length()").value(1))
        .andExpect(jsonPath("$.plans[0].zoneId").value(altoPeru))
        .andExpect(
            jsonPath("$.plans[0].organizationId")
                .value(organization.organizationId().toString()))
        .andExpect(jsonPath("$.plans[0].status").value("PROPOSED"))
        .andExpect(jsonPath("$.plans[0].coverage").value("FULL"))
        .andExpect(jsonPath("$.plans[0].modelVersion").value(FakeAiService.MODEL_VERSION))
        .andExpect(
            jsonPath("$.plans[0].items[*].resourceId").value(containsInAnyOrder(water, food)))
        .andExpect(jsonPath("$.plans[0].items[*].quantity").value(containsInAnyOrder(500, 300)))
        .andExpect(jsonPath("$.plans[0].decision").isEmpty())
        .andExpect(jsonPath("$.unattendedZones[0].zoneId").value(pedregal))
        .andExpect(jsonPath("$.unattendedZones[0].zoneName").value("Quebrada Pedregal"))
        .andExpect(jsonPath("$.unattendedZones[0].coverage").value("NONE"))
        .andExpect(jsonPath("$.excludedZones[0].zoneId").value(castilla))
        .andExpect(jsonPath("$.excludedZones[0].reason").value("NO_NEEDS"))
        .andExpect(jsonPath("$.justification").isNotEmpty());

    mockMvc
        .perform(get("/api/v1/inventories").with(organization.administrator()))
        .andExpect(jsonPath("$.stockItems[*].available").value(contains(0, 0)))
        .andExpect(jsonPath("$.stockItems[*].reserved").value(contains(500, 300)));
  }

  @Test
  void newRecommendationWaitsWhileThereAreProposedPlans() throws Exception {
    recommend(organization.authority()).andExpect(status().isCreated());

    recommend(organization.authority())
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Emergency "
                        + emergencyId
                        + " has proposed distribution plans. Approve or reject them before"
                        + " asking for a new recommendation."));
  }

  @Test
  void modifiedPlanKeepsTheRecommendationAndIsApprovedOnlyOnce() throws Exception {
    String planId = recommendedPlan();

    send(
            patch("/api/v1/distribution-plans/" + planId + "/items"),
            organization.authority(),
            """
            {
              "items": [
                { "resourceId": "%s", "quantity": 200 },
                { "resourceId": "%s", "quantity": 0 }
              ],
              "reason": "La zona recibió víveres de otra ONG"
            }
            """
                .formatted(water, food))
        .andExpect(status().isOk())
        .andExpect(jsonPath(item(water) + ".recommendedQuantity").value(contains(500)))
        .andExpect(jsonPath(item(water) + ".quantity").value(contains(200)))
        .andExpect(jsonPath(item(food) + ".recommendedQuantity").value(contains(300)))
        .andExpect(jsonPath(item(food) + ".quantity").value(contains(0)))
        .andExpect(
            jsonPath("$.modification.modifiedBy").value(organization.authorityId().toString()))
        .andExpect(
            jsonPath("$.modification.reason").value("La zona recibió víveres de otra ONG"));
    mockMvc
        .perform(get("/api/v1/inventories").with(organization.administrator()))
        .andExpect(jsonPath("$.stockItems[*].available").value(contains(300, 300)));

    approve(planId, organization.authority())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.decision.decidedBy").value(organization.authorityId().toString()))
        .andExpect(jsonPath("$.decision.decidedAt").isNotEmpty());
    approve(planId, organization.authority())
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value("Distribution plan " + planId + " is already approved."));
  }

  @Test
  void modificationBeyondTheStockOrWithoutReasonChangesNothing() throws Exception {
    String planId = recommendedPlan();
    String items = "/api/v1/distribution-plans/" + planId + "/items";

    send(
            patch(items),
            organization.authority(),
            "{\"items\": [{\"resourceId\": \"%s\", \"quantity\": 501}], \"reason\": \"Más agua\""
                    .formatted(water)
                + "}")
        .andExpect(status().isConflict());
    send(patch(items), organization.authority(), "{\"items\": [], \"reason\": \"\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("items", "reason")));

    mockMvc
        .perform(get("/api/v1/distribution-plans/" + planId).with(organization.auditor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath(item(water) + ".quantity").value(contains(500)))
        .andExpect(jsonPath("$.modification").isEmpty());
  }

  @Test
  void rejectionNeedsAReasonReleasesTheStockAndStaysInTheHistoryOfTheZone() throws Exception {
    String planId = recommendedPlan();
    String reject = "/api/v1/distribution-plans/" + planId + "/reject";

    send(post(reject), organization.authority(), "{\"reason\": \" \"}")
        .andExpect(status().isBadRequest());
    send(post(reject), organization.authority(), "{\"reason\": \"La vía está bloqueada\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.rejectedReason").value("La vía está bloqueada"));

    mockMvc
        .perform(get("/api/v1/inventories").with(organization.administrator()))
        .andExpect(jsonPath("$.stockItems[*].available").value(contains(500, 300)));
    mockMvc
        .perform(
            get("/api/v1/zones/" + altoPeru + "/distribution-plans").with(organization.auditor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(planId))
        .andExpect(jsonPath("$[0].rejectedReason").value("La vía está bloqueada"));
    mockMvc
        .perform(
            get("/api/v1/emergencies/" + emergencyId + "/distribution-plans")
                .param("status", "REJECTED")
                .with(organization.auditor()))
        .andExpect(jsonPath("$[*].id").value(contains(planId)));
    approve(planId, organization.authority()).andExpect(status().isConflict());
  }

  @Test
  void onlyAnAuthorityOfTheOrganizationDecidesItsPlans() throws Exception {
    String planId = recommendedPlan();
    SignedInOrganization other = SignedInOrganization.register(mockMvc);

    recommend(organization.auditor()).andExpect(status().isForbidden());
    approve(planId, organization.auditor()).andExpect(status().isForbidden());
    approve(planId, other.authority()).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/distribution-plans/" + planId).with(other.auditor()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/distribution-plans/" + planId).with(anonymous()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void recommendationNeedsPrioritizedZonesStockAndTheAiService() throws Exception {
    String emptyEmergency = registerEmergency(organization);
    mockMvc
        .perform(
            post("/api/v1/emergencies/" + emptyEmergency + "/distribution-plans")
                .with(organization.authority()))
        .andExpect(status().isUnprocessableContent());

    aiService.becomeUnavailable();
    recommend(organization.authority()).andExpect(status().isServiceUnavailable());
    aiService.becomeAvailable();
    mockMvc
        .perform(get("/api/v1/inventories").with(organization.administrator()))
        .andExpect(jsonPath("$.stockItems[*].reserved").value(contains(0, 0)));

    SignedInOrganization withoutInventory = SignedInOrganization.register(mockMvc);
    mockMvc
        .perform(
            post("/api/v1/emergencies/" + emergencyId + "/distribution-plans")
                .with(withoutInventory.authority()))
        .andExpect(status().isUnprocessableContent());
  }

  private String recommendedPlan() throws Exception {
    return JsonPath.read(
        recommend(organization.authority())
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.plans[0].id");
  }

  private ResultActions recommend(RequestPostProcessor user) throws Exception {
    return mockMvc.perform(
        post("/api/v1/emergencies/" + emergencyId + "/distribution-plans").with(user));
  }

  private ResultActions approve(String planId, RequestPostProcessor user) throws Exception {
    return mockMvc.perform(post("/api/v1/distribution-plans/" + planId + "/approve").with(user));
  }

  private String resource(String inventoryId, String name, String category, int quantity)
      throws Exception {
    String resourceId =
        JsonPath.read(
            send(
                    post("/api/v1/inventories/" + inventoryId + "/resources"),
                    organization.administrator(),
                    "{\"name\": \"%s\", \"category\": \"%s\", \"unit\": \"UNIT\"}"
                        .formatted(name, category))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.resourceId");
    send(
            post("/api/v1/inventories/" + inventoryId + "/resources/" + resourceId + "/restock"),
            organization.administrator(),
            "{\"quantity\": " + quantity + "}")
        .andExpect(status().isOk());
    return resourceId;
  }

  private String registerEmergency(SignedInOrganization members) throws Exception {
    return idOf(
        send(
            post("/api/v1/emergencies"),
            members.authority(),
            """
            {
              "name": "Huaico Chosica 2026",
              "type": "LANDSLIDE",
              "location": {
                "department": "Lima",
                "province": "Lima",
                "district": "Lurigancho-Chosica"
              },
              "startDate": "2026-03-14"
            }
            """));
  }

  private String zone(SignedInOrganization members, String emergency, String name, String damage)
      throws Exception {
    return idOf(
        send(
            post("/api/v1/emergencies/" + emergency + "/zones"),
            members.authority(),
            """
            {
              "name": "%s",
              "populationImpact": { "population": 600, "familiesAffected": 75,
                                    "peopleAffected": 300 },
              "damageLevel": "%s"
            }
            """
                .formatted(name, damage)));
  }

  /** A zone with a structured field report that mentions these needs, so it has a score. */
  private String scoredZone(
      SignedInOrganization members, String emergency, String name, String damage, Need... needs)
      throws Exception {
    String zoneId = zone(members, emergency, name, damage);
    String text = "Reporte de " + name + " " + zoneId;
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.valueOf(damage), null, null, null, null, null, Set.of(needs), List.of()));
    send(
            post("/api/v1/zones/" + zoneId + "/field-reports"),
            members.authority(),
            "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.processed").value(true));
    mockMvc
        .perform(get("/api/v1/zones/" + zoneId + "/score").with(members.auditor()))
        .andExpect(jsonPath("$.score").isNumber());
    return zoneId;
  }

  private static String item(String resourceId) {
    return "$.items[?(@.resourceId == '" + resourceId + "')]";
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
