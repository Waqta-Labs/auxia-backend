package com.waqtalabs.auxia.acceptance;

import static com.waqtalabs.auxia.TestUsers.organizationAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Steps of US-10. The administrator of a new organization manages the inventory. */
public class StockAlertSteps {

  @Autowired private MockMvc mockMvc;

  private final RequestPostProcessor administrator = organizationAdmin(UUID.randomUUID());
  private String inventoryId;
  private String resourceId;
  private int available;

  @Dado(
      "un inventario con el recurso {string} de stock mínimo {int} y {int} unidades disponibles")
  public void anInventoryWithAMinimum(String name, int minimum, int quantity) throws Exception {
    inventoryId = read(send(post("/api/v1/inventories"), "{\"name\": \"Almacén\"}"), "$.id");
    resourceId =
        read(
            send(
                post("/api/v1/inventories/" + inventoryId + "/resources"),
                """
                {"name": "%s", "category": "WATER", "unit": "UNIT", "minimumStock": %d}
                """
                    .formatted(name, minimum)),
            "$.resourceId");
    restock(quantity);
    available = quantity;
  }

  @Cuando("la cantidad disponible de {string} baja a {int}")
  @Dado("que la cantidad disponible de {string} bajó a {int}")
  public void theAvailableQuantityFallsTo(String name, int quantity) throws Exception {
    send(
        patch("/api/v1/inventories/" + inventoryId + "/resources/" + resourceId + "/adjustment"),
        "{\"quantityChange\": %d, \"reason\": \"Merma\"}".formatted(quantity - available));
    available = quantity;
  }

  @Cuando("se reponen {int} unidades de {string}")
  public void restockOf(int quantity, String name) throws Exception {
    restock(quantity);
  }

  @Entonces("se genera una alerta abierta para {string} con {int} unidades disponibles")
  public void anOpenAlertIsRaised(String name, int quantity) throws Exception {
    List<Map<String, Object>> open = alerts("OPEN");
    assertThat(open).hasSize(1);
    assertThat(open.getFirst())
        .containsEntry("resourceName", name)
        .containsEntry("availableQuantity", quantity);
  }

  @Entonces("la alerta de {string} queda resuelta")
  public void theAlertIsResolved(String name) throws Exception {
    assertThat(alerts("OPEN")).isEmpty();
    assertThat(alerts("RESOLVED"))
        .anySatisfy(alert -> assertThat(alert).containsEntry("resourceName", name));
  }

  private void restock(int quantity) throws Exception {
    send(
        post("/api/v1/inventories/" + inventoryId + "/resources/" + resourceId + "/restock"),
        "{\"quantity\": " + quantity + "}");
  }

  private List<Map<String, Object>> alerts(String status) throws Exception {
    return JsonPath.read(
        mockMvc
            .perform(
                get("/api/v1/inventories/" + inventoryId + "/alerts")
                    .param("status", status)
                    .with(administrator))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$");
  }

  private String send(MockHttpServletRequestBuilder request, String body) throws Exception {
    return mockMvc
        .perform(request.with(administrator).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static String read(String body, String path) {
    return JsonPath.read(body, path);
  }
}
