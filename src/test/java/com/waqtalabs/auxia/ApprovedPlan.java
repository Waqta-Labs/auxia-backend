package com.waqtalabs.auxia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * A distribution plan approved through the API, as the tests of the deliveries need it. The
 * organization has 500 units of water and 300 food kits, one emergency with a zone that needs
 * both, and one brigade with a leader. The plan gives everything to the zone, and its approval
 * assigns the brigade and opens the delivery.
 */
public record ApprovedPlan(
    SignedInOrganization organization,
    String planId,
    String zoneId,
    String water,
    String food,
    String brigadeId) {

  public static ApprovedPlan prepare(MockMvc mockMvc, FakeAiService aiService) throws Exception {
    return prepare(mockMvc, aiService, "Sector Alto Perú");
  }

  /** The same plan, for a zone with another name. */
  public static ApprovedPlan prepare(MockMvc mockMvc, FakeAiService aiService, String zoneName)
      throws Exception {
    var organization = SignedInOrganization.register(mockMvc);
    var api = new Api(mockMvc, organization);
    String inventoryId = api.idOf(api.send(post("/api/v1/inventories"), "{\"name\": \"Lima\"}"));
    String water = api.resource(inventoryId, "Agua 2.5 L", "WATER", 500);
    String food = api.resource(inventoryId, "Kit de alimentos", "FOOD", 300);
    String brigadeId = api.brigade("Brigada Chosica 1");
    String emergencyId = api.emergency();
    String zoneId = api.scoredZone(aiService, emergencyId, zoneName, Need.WATER, Need.FOOD);
    String planId =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/v1/emergencies/" + emergencyId + "/distribution-plans")
                        .with(organization.authority()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.plans[0].id");
    mockMvc
        .perform(
            post("/api/v1/distribution-plans/" + planId + "/approve")
                .with(organization.authority()))
        .andExpect(status().isOk());
    return new ApprovedPlan(organization, planId, zoneId, water, food, brigadeId);
  }

  private record Api(MockMvc mockMvc, SignedInOrganization organization) {

    String resource(String inventoryId, String name, String category, int quantity)
        throws Exception {
      String resourceId =
          JsonPath.read(
              send(
                      post("/api/v1/inventories/" + inventoryId + "/resources"),
                      "{\"name\": \"%s\", \"category\": \"%s\", \"unit\": \"UNIT\"}"
                          .formatted(name, category))
                  .andReturn()
                  .getResponse()
                  .getContentAsString(),
              "$.resourceId");
      send(
              post("/api/v1/inventories/" + inventoryId + "/resources/" + resourceId + "/restock"),
              "{\"quantity\": " + quantity + "}")
          .andExpect(status().isOk());
      return resourceId;
    }

    String brigade(String name) throws Exception {
      String brigadeId =
          idOf(
              send(
                  post("/api/v1/brigades"),
                  """
                  {"name": "%s", "baseLocation": {"latitude": -11.9436, "longitude": -76.7078}}
                  """
                      .formatted(name)));
      send(
              post("/api/v1/brigades/" + brigadeId + "/members"),
              "{\"fullName\": \"Rosa Quispe\", \"role\": \"LEADER\"}")
          .andExpect(status().isCreated());
      return brigadeId;
    }

    String emergency() throws Exception {
      return idOf(
          send(
              post("/api/v1/emergencies"),
              organization.authority(),
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

    /** A zone with a structured field report that mentions these needs, so it has a score. */
    String scoredZone(FakeAiService aiService, String emergencyId, String name, Need... needs)
        throws Exception {
      String zoneId =
          idOf(
              send(
                  post("/api/v1/emergencies/" + emergencyId + "/zones"),
                  organization.authority(),
                  """
                  {
                    "name": "%s",
                    "populationImpact": { "population": 600, "familiesAffected": 75,
                                          "peopleAffected": 300 },
                    "damageLevel": "CRITICAL"
                  }
                  """
                      .formatted(name)));
      String text = "Reporte de " + name + " " + zoneId;
      aiService.willStructure(
          text,
          new StructuredFieldData(
              DamageLevel.CRITICAL, null, null, null, null, null, Set.of(needs), List.of()));
      send(
              post("/api/v1/zones/" + zoneId + "/field-reports"),
              organization.authority(),
              "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text))
          .andExpect(status().isCreated());
      mockMvc
          .perform(get("/api/v1/zones/" + zoneId + "/score").with(organization.auditor()))
          .andExpect(status().isOk());
      return zoneId;
    }

    /** As the administrator, who manages the inventory and the brigades. */
    ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
      return send(request, organization.administrator(), body);
    }

    ResultActions send(
        MockHttpServletRequestBuilder request, RequestPostProcessor user, String body)
        throws Exception {
      return mockMvc.perform(
          request.with(user).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    String idOf(ResultActions result) throws Exception {
      return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
  }
}
