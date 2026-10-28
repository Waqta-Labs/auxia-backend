package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Steps of US-11 to US-14. Each scenario registers an organization whose members sign in, so the
 * check of the current role with Identity Access runs. Field reports are structured by the fake AI
 * Service with the needs of each zone, and its recommendation gives each resource to the most
 * urgent zone that needs it.
 */
public class DistributionPlanSteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  private final Map<String, String> resourceIds = new HashMap<>();
  private final Map<String, Integer> initialStock = new HashMap<>();
  private final Map<String, String> zoneIds = new HashMap<>();
  private SignedInOrganization organization;
  private String inventoryId;
  private String emergencyId;
  private String planId;
  private MockHttpServletResponse response;

  @Before
  public void resetAiService() {
    aiService.reset();
  }

  @Dado("un inventario con estos recursos disponibles:")
  public void anInventoryWithTheseAvailableResources(DataTable resources) throws Exception {
    for (Map<String, String> row : resources.asMaps()) {
      int available = Integer.parseInt(row.get("disponible"));
      addResource(row.get("recurso"), row.get("categoría"), available);
    }
  }

  @Y("estas zonas priorizadas:")
  public void thesePrioritizedZones(DataTable zones) throws Exception {
    for (Map<String, String> row : zones.asMaps()) {
      Need[] needs =
          Arrays.stream(row.get("necesidades").split(","))
              .map(String::strip)
              .map(Need::valueOf)
              .toArray(Need[]::new);
      prioritizedZone(row.get("zona"), row.get("daño"), needs);
    }
  }

  @Dado("una recomendación generada para la zona {string}")
  public void aRecommendationForTheZone(String zone) throws Exception {
    addResource("Agua 2.5 L", "WATER", 500);
    addResource("Kit de alimentos", "FOOD", 300);
    prioritizedZone(zone, "CRITICAL", Need.WATER, Need.FOOD);
    theAuthorityAsksForARecommendation();
    assertThat(response.getStatus()).isEqualTo(201);
    planId = JsonPath.read(response.getContentAsString(), "$.plans[0].id");
  }

  @Cuando("la autoridad solicita una recomendación")
  public void theAuthorityAsksForARecommendation() throws Exception {
    response = send(post("/api/v1/emergencies/" + emergencyId + "/distribution-plans"), null);
  }

  @Dado("que la autoridad aprobó la recomendación")
  @Cuando("la autoridad aprueba la recomendación")
  public void theAuthorityApproves() throws Exception {
    response = send(post(plan() + "/approve"), null);
  }

  @Dado("que la autoridad modificó la cantidad de {string} a {int}")
  @Cuando("la autoridad modifica la cantidad de {string} a {int}")
  public void theAuthorityChangesTheQuantity(String resource, int quantity) throws Exception {
    response =
        send(
            patch(plan() + "/items"),
            """
            {
              "items": [{ "resourceId": "%s", "quantity": %d }],
              "reason": "La zona recibió ayuda de otra ONG"
            }
            """
                .formatted(resourceIds.get(resource), quantity));
  }

  @Cuando("la autoridad rechaza la recomendación sin justificación")
  public void theAuthorityRejectsWithoutJustification() throws Exception {
    response = send(post(plan() + "/reject"), "{\"reason\": \"\"}");
  }

  @Cuando("la autoridad modifica la recomendación sin justificación")
  public void theAuthorityModifiesWithoutJustification() throws Exception {
    response =
        send(
            patch(plan() + "/items"),
            "{\"items\": [{\"resourceId\": \"%s\", \"quantity\": 100}], \"reason\": \" \"}"
                .formatted(resourceIds.get("Agua 2.5 L")));
  }

  @Dado("que la autoridad rechazó la recomendación con la justificación {string}")
  public void theAuthorityRejected(String reason) throws Exception {
    response = send(post(plan() + "/reject"), "{\"reason\": \"%s\"}".formatted(reason));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Cuando("se consulta el historial de la zona {string}")
  public void theHistoryOfTheZoneIsRequested(String zone) throws Exception {
    response =
        mockMvc
            .perform(
                get("/api/v1/zones/" + zoneIds.get(zone) + "/distribution-plans")
                    .with(organization.auditor()))
            .andReturn()
            .getResponse();
  }

  @Entonces("el sistema genera una propuesta que no excede el inventario disponible")
  public void theProposalDoesNotExceedTheInventory() throws Exception {
    assertThat(response.getStatus()).isEqualTo(201);
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<Object>>read(body, "$.plans")).isNotEmpty();
    for (Map.Entry<String, String> resource : resourceIds.entrySet()) {
      List<Integer> quantities =
          JsonPath.read(
              body, "$.plans[*].items[?(@.resourceId == '" + resource.getValue() + "')].quantity");
      assertThat(quantities.stream().mapToInt(Integer::intValue).sum())
          .isLessThanOrEqualTo(initialStock.get(resource.getKey()));
    }
    assertThat(availableStock().values()).allMatch(available -> available >= 0);
  }

  @Entonces("el sistema indica que la zona {string} quedaría totalmente desatendida")
  public void theZoneIsTotallyUnattended(String zone) throws Exception {
    assertThat(response.getStatus()).isEqualTo(201);
    String body = response.getContentAsString();
    String unattended = "$.unattendedZones[?(@.zoneName == '" + zone + "')]";
    assertThat(JsonPath.<List<String>>read(body, unattended + ".coverage")).containsExactly("NONE");
    assertThat(JsonPath.<List<String>>read(body, "$.plans[*].zoneId"))
        .doesNotContain(zoneIds.get(zone));
  }

  @Entonces("la distribución queda habilitada para su ejecución")
  public void theDistributionIsEnabled() throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.status"))
        .isEqualTo("APPROVED");
  }

  @Y("se registra qué autoridad la aprobó")
  public void theApprovingAuthorityIsRecorded() throws Exception {
    String body = response.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.decision.decidedBy"))
        .isEqualTo(organization.authorityId().toString());
    assertThat(JsonPath.<String>read(body, "$.decision.decidedAt")).isNotBlank();
  }

  @Entonces("el sistema indica que ya se encuentra aprobada")
  public void theSystemSaysItIsAlreadyApproved() throws Exception {
    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.detail"))
        .isEqualTo("Distribution plan " + planId + " is already approved.");
  }

  @Entonces("la modificación es aceptada")
  public void theModificationIsAccepted() {
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Entonces("la modificación es rechazada por exceder el inventario disponible")
  public void theModificationExceedsTheInventory() throws Exception {
    assertThat(response.getStatus()).isEqualTo(409);
    String water = resourceIds.get("Agua 2.5 L");
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.detail"))
        .isEqualTo("Resource " + water + " has 500 available and 501 were requested.");
    assertThat(currentQuantityOf(water)).containsExactly(500);
  }

  @Entonces(
      "el plan conserva para {string} la cantidad recomendada de {int}"
          + " y la aprobada de {int}")
  public void thePlanKeepsBothQuantities(String resource, int recommended, int approved)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    String item = "$.items[?(@.resourceId == '" + resourceIds.get(resource) + "')]";
    String body = response.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("APPROVED");
    assertThat(JsonPath.<List<Integer>>read(body, item + ".recommendedQuantity"))
        .containsExactly(recommended);
    assertThat(JsonPath.<List<Integer>>read(body, item + ".quantity")).containsExactly(approved);
  }

  @Entonces("el sistema exige el registro de una justificación")
  public void aJustificationIsRequired() throws Exception {
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(JsonPath.<List<String>>read(response.getContentAsString(), "$.errors[*].field"))
        .contains("reason");
  }

  @Y("la recomendación sigue pendiente de decisión")
  public void theRecommendationIsStillPending() throws Exception {
    MockHttpServletResponse plan =
        mockMvc.perform(get(plan()).with(organization.auditor())).andReturn().getResponse();
    String body = plan.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("PROPOSED");
    assertThat(JsonPath.<Object>read(body, "$.modification")).isNull();
  }

  @Entonces("la justificación {string} queda asociada al plan rechazado")
  public void theJustificationIsInTheHistory(String reason) throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    String rejected = "$[?(@.id == '" + planId + "')]";
    String body = response.getContentAsString();
    assertThat(JsonPath.<List<String>>read(body, rejected + ".status")).containsExactly("REJECTED");
    assertThat(JsonPath.<List<String>>read(body, rejected + ".rejectedReason"))
        .containsExactly(reason);
  }

  private void addResource(String name, String category, int available) throws Exception {
    if (inventoryId == null) {
      inventoryId =
          idOf(
              send(post("/api/v1/inventories"), "{\"name\": \"Almacén Lima\"}", administrator()));
    }
    String resourceId =
        JsonPath.read(
            send(
                    post("/api/v1/inventories/" + inventoryId + "/resources"),
                    "{\"name\": \"%s\", \"category\": \"%s\", \"unit\": \"UNIT\"}"
                        .formatted(name, category),
                    administrator())
                .getContentAsString(),
            "$.resourceId");
    send(
        post("/api/v1/inventories/" + inventoryId + "/resources/" + resourceId + "/restock"),
        "{\"quantity\": " + available + "}",
        administrator());
    resourceIds.put(name, resourceId);
    initialStock.put(name, available);
  }

  /** A zone with a structured field report that mentions these needs, so it has a score. */
  private void prioritizedZone(String name, String damage, Need... needs) throws Exception {
    if (emergencyId == null) {
      emergencyId =
          idOf(
              send(
                  post("/api/v1/emergencies"),
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
    String zoneId =
        idOf(
            send(
                post("/api/v1/emergencies/" + emergencyId + "/zones"),
                """
                {
                  "name": "%s",
                  "populationImpact": { "population": 600, "familiesAffected": 75,
                                        "peopleAffected": 300 },
                  "damageLevel": "%s"
                }
                """
                    .formatted(name, damage)));
    String text = "Reporte de campo de " + name;
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.valueOf(damage), null, null, null, null, null, Set.of(needs), List.of()));
    MockHttpServletResponse report =
        send(
            post("/api/v1/zones/" + zoneId + "/field-reports"),
            "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text));
    assertThat(JsonPath.<Boolean>read(report.getContentAsString(), "$.processed")).isTrue();
    zoneIds.put(name, zoneId);
  }

  private Map<String, Integer> availableStock() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/v1/inventories").with(administrator()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<String> names = JsonPath.read(body, "$.stockItems[*].name");
    List<Integer> available = JsonPath.read(body, "$.stockItems[*].available");
    return names.stream()
        .collect(Collectors.toMap(name -> name, name -> available.get(names.indexOf(name))));
  }

  /** Quantity of the resource in the stored plan, as a list of one element. */
  private List<Integer> currentQuantityOf(String resourceId) throws Exception {
    String body =
        mockMvc
            .perform(get(plan()).with(organization.auditor()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.items[?(@.resourceId == '" + resourceId + "')].quantity");
  }

  private String plan() {
    return "/api/v1/distribution-plans/" + planId;
  }

  private RequestPostProcessor administrator() throws Exception {
    return organization().administrator();
  }

  private SignedInOrganization organization() throws Exception {
    if (organization == null) {
      organization = SignedInOrganization.register(mockMvc);
    }
    return organization;
  }

  /** A request of the authority of the organization. */
  private MockHttpServletResponse send(MockHttpServletRequestBuilder request, String body)
      throws Exception {
    return send(request, body, organization().authority());
  }

  private MockHttpServletResponse send(
      MockHttpServletRequestBuilder request, String body, RequestPostProcessor user)
      throws Exception {
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mockMvc.perform(request.with(user)).andReturn().getResponse();
  }

  private static String idOf(MockHttpServletResponse response) throws Exception {
    return JsonPath.read(response.getContentAsString(), "$.id");
  }
}
