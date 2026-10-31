package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.SignedInOrganization.SignedInUser;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Steps of US-20 and US-23. The first zone has its population, so the fake AI Service scores it
 * when it is registered; the second one has no population and keeps no score.
 */
public class ShiftHandoverSteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  private final Map<String, String> zones = new HashMap<>();
  private final Map<String, Object> scores = new HashMap<>();
  private SignedInOrganization organization;
  private SignedInUser next;
  private String history;
  private String zonesInCharge;

  @Before
  public void resetAiService() {
    aiService.reset();
  }

  @Dado("una autoridad que sigue las zonas {string} y {string}")
  public void anAuthorityFollowingTwoZones(String first, String second) throws Exception {
    organization = SignedInOrganization.register(mockMvc);
    String emergency =
        idOf(
            send(
                post("/api/v1/emergencies"),
                organization.authority(),
                """
                {"name": "Huaico Chosica 2026", "type": "LANDSLIDE",
                 "location": {"department": "Lima", "province": "Lima",
                              "district": "Lurigancho-Chosica"},
                 "startDate": "2026-03-14"}
                """));
    zone(emergency, first, "\"population\": 600, ");
    zone(emergency, second, "");
  }

  @Y("la autoridad prioriza a mano la zona {string} porque {string}")
  public void theAuthorityPrioritizes(String zone, String reason) throws Exception {
    assertThat(
            send(
                    put("/api/v1/zones/" + zones.get(zone) + "/manual-priority"),
                    organization.authority(),
                    "{\"reason\": \"%s\"}".formatted(reason))
                .getResponse()
                .getStatus())
        .isEqualTo(200);
  }

  @Y("la autoridad le quita la prioridad manual a la zona {string}")
  public void theAuthorityRemovesThePriority(String zone) throws Exception {
    assertThat(
            mockMvc
                .perform(
                    delete("/api/v1/zones/" + zones.get(zone) + "/manual-priority")
                        .with(organization.authority()))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
  }

  @Cuando("la autoridad registra el cambio de turno con otra autoridad")
  public void theAuthorityHandsOverTheShift() throws Exception {
    next = organization.anotherAuthority(mockMvc);
    assertThat(
            send(
                    post("/api/v1/shift-handovers"),
                    organization.authority(),
                    """
                    {"incomingAuthorityId": "%s", "notes": "Alto Perú sigue sin acceso"}
                    """
                        .formatted(next.id()))
                .getResponse()
                .getStatus())
        .isEqualTo(201);
  }

  @Cuando("se consulta el registro de cambios de la zona {string}")
  public void theHistoryIsQueried(String zone) throws Exception {
    history = history(zone, organization.auditor());
  }

  @Y("la nueva autoridad consulta el registro de cambios de la zona {string}")
  public void theNextAuthorityQueriesTheHistory(String zone) throws Exception {
    history = history(zone, next.token());
  }

  @Entonces("el registro de cambios lista en orden {string}")
  public void theHistoryListsInOrder(String types) throws Exception {
    assertThat(JsonPath.<List<String>>read(history, "$[*].type"))
        .containsExactlyElementsOf(Arrays.asList(types.split(", ")));
  }

  @Y("cada decisión muestra a la autoridad que la tomó")
  public void eachDecisionShowsItsAuthority() {
    assertThat(JsonPath.<List<String>>read(history, "$[*].decidedBy"))
        .containsOnly(organization.authorityId().toString());
    assertThat(JsonPath.<List<String>>read(history, "$[*].decidedByName"))
        .containsOnly("Rosa Quispe");
    assertThat(JsonPath.<List<String>>read(history, "$[*].decidedAt")).doesNotContainNull();
  }

  @Y("las decisiones del turno anterior muestran a la autoridad anterior")
  public void thePreviousShiftShowsThePreviousAuthority() {
    assertThat(JsonPath.<List<String>>read(history, "$[*].decidedBy"))
        .containsOnly(organization.authorityId().toString());
    assertThat(JsonPath.<String>read(history, "$[3].handedOverTo"))
        .isEqualTo(next.id().toString());
  }

  @Entonces("la nueva autoridad queda como responsable de las zonas {string} y {string}")
  public void theNextAuthorityIsResponsible(String first, String second) throws Exception {
    String body = inCharge(next.token());
    assertThat(JsonPath.<List<String>>read(body, "$[*].id"))
        .containsExactly(zones.get(first), zones.get(second));
    assertThat(JsonPath.<List<String>>read(body, "$[*].responsibleAuthorityId"))
        .containsOnly(next.id().toString());
  }

  @Y("la autoridad anterior ya no tiene zonas a su cargo")
  public void thePreviousAuthorityHasNoZones() throws Exception {
    assertThat(JsonPath.<List<Object>>read(inCharge(organization.authority()), "$")).isEmpty();
  }

  @Y("la nueva autoridad inicia sesión y consulta las zonas a su cargo")
  public void theNextAuthorityQueriesItsZones() throws Exception {
    zonesInCharge = inCharge(next.token());
  }

  @Entonces("ve la zona {string} en estado {string} con el mismo score")
  public void seesTheZoneWithItsLastStatus(String zone, String status) {
    String path = "$[?(@.id == '" + zones.get(zone) + "')]";
    assertThat(JsonPath.<List<String>>read(zonesInCharge, path + ".status"))
        .containsExactly(status);
    assertThat(JsonPath.<List<Object>>read(zonesInCharge, path + ".score"))
        .containsExactly(scores.get(zone));
  }

  private void zone(String emergency, String name, String population) throws Exception {
    String body =
        send(
                post("/api/v1/emergencies/" + emergency + "/zones"),
                organization.authority(),
                """
                {"name": "%s", "damageLevel": "SEVERE",
                 "populationImpact": {%s"familiesAffected": 35, "peopleAffected": 140}}
                """
                    .formatted(name, population))
            .getResponse()
            .getContentAsString();
    zones.put(name, JsonPath.read(body, "$.id"));
    scores.put(name, JsonPath.read(body, "$.score"));
  }

  private String history(String zone, RequestPostProcessor user) throws Exception {
    return mockMvc
        .perform(get("/api/v1/zones/" + zones.get(zone) + "/history").with(user))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String inCharge(RequestPostProcessor user) throws Exception {
    return mockMvc
        .perform(get("/api/v1/zones/in-charge").with(user))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private MvcResult send(
      MockHttpServletRequestBuilder request, RequestPostProcessor user, String body)
      throws Exception {
    return mockMvc
        .perform(request.with(user).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn();
  }

  private static String idOf(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
