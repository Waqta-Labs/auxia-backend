package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.SignedInOrganization.SignedInUser;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import java.util.UUID;
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
 * History of decisions of a zone (US-20) and shift changes between authorities (US-23) through the
 * API. Users sign in, because a shift change asks Identity Access for the next authority.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FakeAiServiceConfiguration.class})
class ZoneHistoryControllerTests {

  private static final String EMERGENCY =
      """
      {"name": "%s", "type": "LANDSLIDE",
       "location": {"department": "Lima", "province": "Lima", "district": "Lurigancho-Chosica"},
       "startDate": "2026-03-14"}
      """;
  private static final String ZONE =
      """
      {"name": "%s", "damageLevel": "MODERATE",
       "populationImpact": {"familiesAffected": 5, "peopleAffected": 20}}
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;

  @BeforeEach
  void resetAiService() {
    aiService.reset();
  }

  @Test
  void theHistoryListsEachDecisionInOrderWithItsAuthority() throws Exception {
    ApprovedPlan plan = ApprovedPlan.prepare(mockMvc, aiService);
    SignedInOrganization organization = plan.organization();
    String authority = organization.authorityId().toString();
    send(
            put("/api/v1/zones/" + plan.zoneId() + "/manual-priority"),
            organization.authority(),
            "{\"reason\": \"Riesgo inminente de un nuevo huaico\"}")
        .andExpect(status().isOk());
    mockMvc
        .perform(
            delete("/api/v1/zones/" + plan.zoneId() + "/manual-priority")
                .with(organization.authority()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/zones/" + plan.zoneId() + "/history").with(organization.auditor()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].type")
                .value(
                    contains(
                        "ZONE_REGISTERED",
                        "PLAN_APPROVED",
                        "MANUAL_PRIORITY_SET",
                        "MANUAL_PRIORITY_REMOVED")))
        .andExpect(jsonPath("$[*].decidedBy").value(everyItem(is(authority))))
        .andExpect(jsonPath("$[*].decidedByName").value(everyItem(is("Rosa Quispe"))))
        .andExpect(jsonPath("$[1].subjectId").value(plan.planId()))
        .andExpect(jsonPath("$[2].detail").value("Riesgo inminente de un nuevo huaico"));
  }

  @Test
  void theNextAuthorityReceivesTheActiveZonesAndTheirHistory() throws Exception {
    SignedInOrganization organization = SignedInOrganization.register(mockMvc);
    SignedInUser next = organization.anotherAuthority(mockMvc);
    String emergency = emergency(organization, "Huaico Chosica 2026");
    String altoPeru = zone(organization, emergency, "Sector Alto Perú");
    String pedregal = zone(organization, emergency, "Quebrada Pedregal");
    String closed = emergency(organization, "Lluvias Chosica 2025");
    zone(organization, closed, "Nicolás de Piérola");
    mockMvc
        .perform(patch("/api/v1/emergencies/" + closed + "/close").with(organization.authority()))
        .andExpect(status().isOk());

    send(
            post("/api/v1/shift-handovers"),
            organization.authority(),
            """
            {"incomingAuthorityId": "%s", "notes": "Alto Perú sigue sin acceso"}
            """
                .formatted(next.id()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.outgoingAuthorityId").value(organization.authorityId().toString()))
        .andExpect(jsonPath("$.incomingAuthorityId").value(next.id().toString()))
        .andExpect(jsonPath("$.zoneIds").value(containsInAnyOrder(altoPeru, pedregal)));

    mockMvc
        .perform(get("/api/v1/zones/in-charge").with(next.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(altoPeru, pedregal)))
        .andExpect(jsonPath("$[*].status").value(everyItem(is("REGISTERED"))))
        .andExpect(
            jsonPath("$[*].responsibleAuthorityId").value(everyItem(is(next.id().toString()))));
    mockMvc
        .perform(get("/api/v1/zones/in-charge").with(organization.authority()))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/api/v1/zones/" + altoPeru + "/history").with(next.token()))
        .andExpect(jsonPath("$[*].type").value(contains("ZONE_REGISTERED", "SHIFT_HANDED_OVER")))
        .andExpect(jsonPath("$[0].decidedBy").value(organization.authorityId().toString()))
        .andExpect(jsonPath("$[1].handedOverTo").value(next.id().toString()))
        .andExpect(jsonPath("$[1].handedOverToName").value("Rosa Quispe"))
        .andExpect(jsonPath("$[1].detail").value("Alto Perú sigue sin acceso"));
  }

  @Test
  void theShiftGoesOnlyToAnotherActiveAuthorityOfTheOrganization() throws Exception {
    SignedInOrganization organization = SignedInOrganization.register(mockMvc);
    SignedInOrganization another = SignedInOrganization.register(mockMvc);
    String handover = "{\"incomingAuthorityId\": \"%s\"}";

    for (UUID incoming :
        new UUID[] {organization.authorityId(), another.authorityId(), UUID.randomUUID()}) {
      send(post("/api/v1/shift-handovers"), organization.authority(), handover.formatted(incoming))
          .andExpect(status().isUnprocessableEntity());
    }
    send(post("/api/v1/shift-handovers"), organization.authority(), "{}")
        .andExpect(status().isBadRequest());
    send(
            post("/api/v1/shift-handovers"),
            organization.auditor(),
            handover.formatted(organization.anotherAuthority(mockMvc).id()))
        .andExpect(status().isForbidden());
  }

  @Test
  void aZoneThatDoesNotExistHasNoHistory() throws Exception {
    SignedInOrganization organization = SignedInOrganization.register(mockMvc);

    mockMvc
        .perform(
            get("/api/v1/zones/" + UUID.randomUUID() + "/history").with(organization.auditor()))
        .andExpect(status().isNotFound());
  }

  private String emergency(SignedInOrganization organization, String name) throws Exception {
    return idOf(
        send(post("/api/v1/emergencies"), organization.authority(), EMERGENCY.formatted(name)));
  }

  private String zone(SignedInOrganization organization, String emergency, String name)
      throws Exception {
    return idOf(
        send(
            post("/api/v1/emergencies/" + emergency + "/zones"),
            organization.authority(),
            ZONE.formatted(name)));
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
