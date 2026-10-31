package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

import static com.waqtalabs.auxia.TestUsers.organizationAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Low stock alerts through the API (US-10). Not transactional: the alerts are evaluated after
 * each change is committed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StockAlertControllerTests {

  @Autowired private MockMvc mockMvc;

  private final UUID organizationId = UUID.randomUUID();
  private final RequestPostProcessor administrator = organizationAdmin(organizationId);

  @Test
  void alertIsRaisedBelowTheMinimumAndResolvedByARestock() throws Exception {
    String inventory = idOf(send(post("/api/v1/inventories"), "{\"name\": \"Almacén Lima\"}"));
    String water =
        JsonPath.read(
            send(
                    post("/api/v1/inventories/" + inventory + "/resources"),
                    """
                    {"name": "Agua 2.5 L", "category": "WATER", "unit": "UNIT",
                     "minimumStock": 50}
                    """)
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.resourceId");

    alerts(inventory, "OPEN")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].resourceId").value(water))
        .andExpect(jsonPath("$[0].resourceName").value("Agua 2.5 L"))
        .andExpect(jsonPath("$[0].availableQuantity").value(0))
        .andExpect(jsonPath("$[0].minimumStock").value(50));

    send(
            post("/api/v1/inventories/" + inventory + "/resources/" + water + "/restock"),
            "{\"quantity\": 80}")
        .andExpect(status().isOk());
    alerts(inventory, "OPEN").andExpect(jsonPath("$.length()").value(0));
    alerts(inventory, "RESOLVED")
        .andExpect(jsonPath("$[0].status").value("RESOLVED"))
        .andExpect(jsonPath("$[0].resolvedAt").exists());

    send(
            patch("/api/v1/inventories/" + inventory + "/resources/" + water + "/adjustment"),
            "{\"quantityChange\": -40, \"reason\": \"Botellas dañadas\"}")
        .andExpect(status().isOk());
    alerts(inventory, "OPEN").andExpect(jsonPath("$[0].availableQuantity").value(40));
    alerts(inventory, null).andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void alertsOfAnotherOrganizationAreNotFound() throws Exception {
    String inventory = idOf(send(post("/api/v1/inventories"), "{\"name\": \"Almacén Lima\"}"));

    mockMvc
        .perform(
            get("/api/v1/inventories/" + inventory + "/alerts")
                .with(organizationAdmin(UUID.randomUUID())))
        .andExpect(status().isNotFound());
  }

  private ResultActions alerts(String inventory, String status) throws Exception {
    var request = get("/api/v1/inventories/" + inventory + "/alerts").with(administrator);
    return mockMvc.perform(status == null ? request : request.param("status", status));
  }

  private ResultActions send(MockHttpServletRequestBuilder request, String body)
      throws Exception {
    return mockMvc.perform(
        request.with(administrator).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }
}
