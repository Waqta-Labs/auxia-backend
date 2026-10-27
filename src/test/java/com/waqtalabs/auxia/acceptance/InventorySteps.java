package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ReservationLineResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ResourceManagementContextFacade;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Steps of US-08, US-09 and TS-03. Each scenario creates the inventory of a new organization. The
 * reservation of a plan goes through ResourceManagementContextFacade, as Emergency Management will
 * do it.
 */
public class InventorySteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private ResourceManagementContextFacade facade;

  private final UUID organizationId = UUID.randomUUID();
  private final Map<String, String> resourceIds = new HashMap<>();
  private String inventoryId;
  private MockHttpServletResponse response;

  @Dado("un inventario con el recurso {string} y {int} unidades disponibles")
  public void anInventoryWithAResource(String resource, int available) throws Exception {
    createInventory();
    registerResource(resource, available);
  }

  @Dado("un inventario con estos recursos:")
  public void anInventoryWithTheseResources(DataTable resources) throws Exception {
    createInventory();
    for (Map<String, String> row : resources.asMaps()) {
      registerResource(row.get("recurso"), Integer.parseInt(row.get("disponible")));
    }
  }

  @Dado("que un plan de distribución reservó {int} unidades de {string}")
  public void aPlanReserved(int quantity, String resource) {
    facade.reserveResources(
        organizationId,
        UUID.randomUUID(),
        List.of(new ReservationLineResource(UUID.fromString(resourceIds.get(resource)), quantity)));
  }

  @Cuando("la autoridad registra el ingreso de {int} unidades de {string}")
  public void theAuthorityRegistersTheEntry(int quantity, String resource) throws Exception {
    send(post(resource(resource) + "/restock"), "{\"quantity\": " + quantity + "}");
  }

  @Cuando("la autoridad consulta el inventario")
  @Cuando("se envía una solicitud GET del inventario")
  public void theInventoryIsRequested() throws Exception {
    response = mockMvc.perform(get("/api/v1/inventories/" + inventoryId)).andReturn().getResponse();
  }

  @Cuando("se envía una solicitud para reducir {string} en {int} unidades")
  public void aReductionIsSent(String resource, int quantity) throws Exception {
    send(
        patch(resource(resource) + "/adjustment"),
        "{\"quantityChange\": -%d, \"reason\": \"Conteo físico\"}".formatted(quantity));
  }

  @Entonces("la cantidad disponible de {string} es {int}")
  public void theAvailableQuantityIs(String resource, int available) throws Exception {
    assertThat(availableByResource().get(resource)).isEqualTo(available);
  }

  @Entonces("el ingreso es rechazado")
  public void theEntryIsRejected() {
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Entonces("se muestra la cantidad disponible de cada recurso:")
  public void theAvailableQuantityOfEachResourceIsShown(DataTable expected) throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    Map<String, Integer> rows = new LinkedHashMap<>();
    expected
        .asMaps()
        .forEach(row -> rows.put(row.get("recurso"), Integer.parseInt(row.get("disponible"))));
    assertThat(availableIn(response)).containsExactlyEntriesOf(rows);
  }

  @Entonces("{string} muestra {int} reservadas y {int} disponibles")
  public void theResourceShowsReservedAndAvailable(String resource, int reserved, int available)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    String item = "$.stockItems[?(@.name == '" + resource + "')]";
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<Integer>>read(body, item + ".reserved")).containsExactly(reserved);
    assertThat(JsonPath.<List<Integer>>read(body, item + ".available")).containsExactly(available);
  }

  @Entonces("el endpoint responde con código {int} y la cantidad disponible de cada recurso")
  public void theEndpointRespondsWithTheAvailableQuantities(int status) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(availableIn(response)).isNotEmpty().doesNotContainValue(null);
  }

  @Entonces("el endpoint responde con código {int} y el inventario no se modifica")
  public void theEndpointRespondsAndNothingChanges(int status) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(availableByResource()).containsEntry("Agua 2.5 L", 100);
  }

  @Y("la cantidad disponible de {string} sigue siendo {int}")
  public void theAvailableQuantityIsStill(String resource, int available) throws Exception {
    theAvailableQuantityIs(resource, available);
  }

  private void createInventory() throws Exception {
    send(
        post("/api/v1/inventories"),
        "{\"organizationId\": \"%s\", \"name\": \"Almacén Central Lima\"}"
            .formatted(organizationId));
    assertThat(response.getStatus()).isEqualTo(201);
    inventoryId = JsonPath.read(response.getContentAsString(), "$.id");
  }

  private void registerResource(String name, int available) throws Exception {
    send(
        post("/api/v1/inventories/" + inventoryId + "/resources"),
        "{\"name\": \"%s\", \"category\": \"OTHER\", \"unit\": \"UNIT\"}".formatted(name));
    assertThat(response.getStatus()).isEqualTo(201);
    resourceIds.put(name, JsonPath.read(response.getContentAsString(), "$.resourceId"));
    send(post(resource(name) + "/restock"), "{\"quantity\": " + available + "}");
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private Map<String, Integer> availableByResource() throws Exception {
    MockHttpServletResponse inventory =
        mockMvc.perform(get("/api/v1/inventories/" + inventoryId)).andReturn().getResponse();
    return availableIn(inventory);
  }

  private static Map<String, Integer> availableIn(MockHttpServletResponse inventory)
      throws Exception {
    String body = inventory.getContentAsString();
    List<String> names = JsonPath.read(body, "$.stockItems[*].name");
    List<Integer> available = JsonPath.read(body, "$.stockItems[*].available");
    Map<String, Integer> result = new LinkedHashMap<>();
    for (int i = 0; i < names.size(); i++) {
      result.put(names.get(i), available.get(i));
    }
    return result;
  }

  private String resource(String name) {
    return "/api/v1/inventories/" + inventoryId + "/resources/" + resourceIds.get(name);
  }

  private void send(MockHttpServletRequestBuilder request, String body) throws Exception {
    response =
        mockMvc
            .perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn()
            .getResponse();
  }
}
