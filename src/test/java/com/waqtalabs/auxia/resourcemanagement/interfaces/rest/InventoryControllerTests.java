package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class InventoryControllerTests {

  static final String INVENTORIES = "/api/v1/inventories";
  static final String WATER =
      """
      {"name": "Agua 2.5 L", "category": "WATER", "unit": "UNIT", "minimumStock": 50}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void createReturnsCreatedWithAnEmptyInventory() throws Exception {
    UUID organizationId = UUID.randomUUID();

    createInventory(organizationId)
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith(INVENTORIES + "/")))
        .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
        .andExpect(jsonPath("$.name").value("Almacén Central Lima"))
        .andExpect(jsonPath("$.stockItems").isEmpty());
  }

  @Test
  void secondInventoryOfAnOrganizationReturnsConflict() throws Exception {
    UUID organizationId = UUID.randomUUID();
    createInventory(organizationId).andExpect(status().isCreated());

    createInventory(organizationId)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value("Organization " + organizationId + " already has an inventory."));
  }

  @Test
  void invalidInventoryReturnsBadRequest() throws Exception {
    mockMvc
        .perform(json(post(INVENTORIES), "{\"name\": \"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("organizationId")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")));
  }

  @Test
  void registeredResourceStartsWithoutStockAndBelowItsMinimum() throws Exception {
    String inventoryId = idOf(createInventory(UUID.randomUUID()));

    mockMvc
        .perform(json(post(resources(inventoryId)), WATER))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.resourceId").isNotEmpty())
        .andExpect(jsonPath("$.name").value("Agua 2.5 L"))
        .andExpect(jsonPath("$.category").value("WATER"))
        .andExpect(jsonPath("$.onHand").value(0))
        .andExpect(jsonPath("$.available").value(0))
        .andExpect(jsonPath("$.minimumStock").value(50))
        .andExpect(jsonPath("$.belowMinimum").value(true));
    mockMvc
        .perform(json(post(resources(inventoryId)), WATER.replace("Agua 2.5 L", "agua 2.5 l")))
        .andExpect(status().isConflict());
  }

  @Test
  void restockUpdatesTheAvailableQuantity() throws Exception {
    String inventoryId = idOf(createInventory(UUID.randomUUID()));
    String resourceId = resourceIdOf(inventoryId, WATER);

    restock(inventoryId, resourceId, "{\"quantity\": 120}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.onHand").value(120))
        .andExpect(jsonPath("$.available").value(120))
        .andExpect(jsonPath("$.belowMinimum").value(false));
  }

  @Test
  void restockWithAnInvalidQuantityIsRejectedAndChangesNothing() throws Exception {
    String inventoryId = idOf(createInventory(UUID.randomUUID()));
    String resourceId = resourceIdOf(inventoryId, WATER);
    restock(inventoryId, resourceId, "{\"quantity\": 10}").andExpect(status().isOk());

    restock(inventoryId, resourceId, "{\"quantity\": -5}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    restock(inventoryId, resourceId, "{}").andExpect(status().isBadRequest());

    mockMvc
        .perform(get(INVENTORIES + "/" + inventoryId))
        .andExpect(jsonPath("$.stockItems[0].available").value(10));
  }

  @Test
  void adjustmentAddsOrRemovesWithinTheAvailableQuantity() throws Exception {
    String inventoryId = idOf(createInventory(UUID.randomUUID()));
    String resourceId = resourceIdOf(inventoryId, WATER);
    restock(inventoryId, resourceId, "{\"quantity\": 100}").andExpect(status().isOk());

    adjust(inventoryId, resourceId, "{\"quantityChange\": -30, \"reason\": \"Botellas rotas\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(70));
    adjust(inventoryId, resourceId, "{\"quantityChange\": -71, \"reason\": \"Pérdida\"}")
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value("Resource " + resourceId + " has 70 available and 71 were requested."));
    adjust(inventoryId, resourceId, "{\"quantityChange\": 0, \"reason\": \"Conteo\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("quantityChange"));
    adjust(inventoryId, resourceId, "{\"quantityChange\": 5}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("reason"));
  }

  @Test
  void inventoryListsItsResourcesByNameAndFiltersByCategory() throws Exception {
    UUID organizationId = UUID.randomUUID();
    String inventoryId = idOf(createInventory(organizationId));
    resourceIdOf(
        inventoryId, "{\"name\": \"Kit de alimentos\", \"category\": \"FOOD\", \"unit\": \"KIT\"}");
    resourceIdOf(inventoryId, WATER);

    mockMvc
        .perform(get(INVENTORIES + "/" + inventoryId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.stockItems[*].name").value(contains("Agua 2.5 L", "Kit de alimentos")));
    mockMvc
        .perform(get(INVENTORIES + "/" + inventoryId).param("category", "FOOD"))
        .andExpect(jsonPath("$.stockItems[*].name").value(contains("Kit de alimentos")));
    mockMvc
        .perform(get(INVENTORIES).param("organizationId", organizationId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventoryId));
  }

  @Test
  void unknownInventoryOrResourceReturnsNotFound() throws Exception {
    String inventoryId = idOf(createInventory(UUID.randomUUID()));
    UUID unknown = UUID.randomUUID();

    mockMvc.perform(get(INVENTORIES + "/" + unknown)).andExpect(status().isNotFound());
    mockMvc
        .perform(get(INVENTORIES).param("organizationId", unknown.toString()))
        .andExpect(status().isNotFound());
    restock(inventoryId, unknown.toString(), "{\"quantity\": 1}")
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Resource " + unknown + " does not exist in inventory " + inventoryId + "."));
  }

  private ResultActions createInventory(UUID organizationId) throws Exception {
    return mockMvc.perform(
        json(
            post(INVENTORIES),
            "{\"organizationId\": \"%s\", \"name\": \"Almacén Central Lima\"}"
                .formatted(organizationId)));
  }

  private String resourceIdOf(String inventoryId, String resource) throws Exception {
    String body =
        mockMvc
            .perform(json(post(resources(inventoryId)), resource))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.resourceId");
  }

  private ResultActions restock(String inventoryId, String resourceId, String body)
      throws Exception {
    return mockMvc.perform(
        json(post(resources(inventoryId) + "/" + resourceId + "/restock"), body));
  }

  private ResultActions adjust(String inventoryId, String resourceId, String body)
      throws Exception {
    return mockMvc.perform(
        json(patch(resources(inventoryId) + "/" + resourceId + "/adjustment"), body));
  }

  private static String resources(String inventoryId) {
    return INVENTORIES + "/" + inventoryId + "/resources";
  }

  private static MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder request, String body) {
    return request.contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }
}
