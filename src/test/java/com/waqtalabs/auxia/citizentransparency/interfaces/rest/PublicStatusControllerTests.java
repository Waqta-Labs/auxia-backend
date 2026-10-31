package com.waqtalabs.auxia.citizentransparency.interfaces.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.FakeBlockchainConfiguration;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The public view through the API, with no token (US-24, US-25, US-26). The plan, its brigade and
 * its delivery come from the real flow, so every stage is projected from the events.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  FakeBlockchainConfiguration.class
})
class PublicStatusControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;

  private ApprovedPlan plan;
  private String zoneName;
  private String deliveryId;

  @BeforeEach
  void approvedPlanWhoseBrigadeIsOnItsWay() throws Exception {
    aiService.reset();
    ledger.reset();
    zoneName = "Sector " + UUID.randomUUID();
    plan = ApprovedPlan.prepare(mockMvc, aiService, zoneName);
    deliveryId =
        JsonPath.read(
            mockMvc
                .perform(
                    get("/api/v1/deliveries")
                        .param("zoneId", plan.zoneId())
                        .with(plan.organization().auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
  }

  @Test
  void anyoneSeesThatAidIsOnItsWayWithoutTheScore() throws Exception {
    zone()
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("max-age=60")))
        .andExpect(header().string("Cache-Control", containsString("public")))
        .andExpect(jsonPath("$.zoneName").value(zoneName))
        .andExpect(jsonPath("$.stage").value("AID_IN_TRANSIT"))
        .andExpect(jsonPath("$.deliveriesCompleted").value(0))
        .andExpect(jsonPath("$.lastDeliveryVerification").value(nullValue()))
        .andExpect(jsonPath("$.score").doesNotExist())
        .andExpect(jsonPath("$.priorityLevel").doesNotExist());
  }

  @Test
  void verifiedDeliveryShowsItsDateAndItsPublicVerification() throws Exception {
    register().andExpect(status().isCreated());

    String verificationUrl =
        JsonPath.read(
            zone()
                .andExpect(jsonPath("$.stage").value("AID_DELIVERED"))
                .andExpect(jsonPath("$.deliveriesCompleted").value(1))
                .andExpect(jsonPath("$.lastDeliveryAt").value("2026-03-15T10:30:00"))
                .andExpect(jsonPath("$.lastDeliveryVerification").value("VERIFIED"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.verificationUrl");

    mockMvc
        .perform(get(verificationUrl))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deliveryId").value(deliveryId))
        .andExpect(jsonPath("$.verified").value(true));
  }

  @Test
  void deliveryNotAnchoredYetIsPendingVerification() throws Exception {
    ledger.becomeUnavailable();

    register().andExpect(status().isCreated());

    zone()
        .andExpect(jsonPath("$.stage").value("AID_DELIVERED"))
        .andExpect(jsonPath("$.lastDeliveryVerification").value("PENDING_VERIFICATION"))
        .andExpect(jsonPath("$.verificationUrl").value(nullValue()));
  }

  @Test
  void emergencyAndItsZonesAreListedForAnyone() throws Exception {
    String emergencyId =
        JsonPath.read(zone().andReturn().getResponse().getContentAsString(), "$.emergencyId");
    String registeredZone =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/v1/emergencies/" + emergencyId + "/zones")
                        .with(plan.organization().authority())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {"name": "Quebrada Pedregal",
                             "populationImpact": {"familiesAffected": 10, "peopleAffected": 40},
                             "damageLevel": "MODERATE"}
                            """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");

    mockMvc
        .perform(get("/api/v1/public/emergencies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(hasItem(emergencyId)));
    mockMvc
        .perform(get("/api/v1/public/emergencies/" + emergencyId + "/zones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].zoneName").value("Quebrada Pedregal"))
        .andExpect(jsonPath("$[0].zoneId").value(registeredZone))
        .andExpect(jsonPath("$[0].stage").value("REGISTERED"))
        .andExpect(jsonPath("$[1].zoneName").value(zoneName));
  }

  @Test
  void citizenFindsTheZoneByDistrictAndPartOfItsName() throws Exception {
    String part = zoneName.substring(8, 20).toUpperCase();

    mockMvc
        .perform(
            get("/api/v1/public/zones")
                .param("district", "LURIGANCHO-CHOSICA")
                .param("name", part))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].zoneId").value(plan.zoneId()))
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc
        .perform(get("/api/v1/public/zones").param("district", "Miraflores").param("name", part))
        .andExpect(jsonPath("$.length()").value(0));
    mockMvc.perform(get("/api/v1/public/zones")).andExpect(status().isBadRequest());
  }

  @Test
  void zoneOrEmergencyNotRegisteredHasNoInformation() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/zones/" + UUID.randomUUID()).header("Accept-Language", "es"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value(containsString("no está registrada")));
    mockMvc
        .perform(get("/api/v1/public/emergencies/" + UUID.randomUUID() + "/zones"))
        .andExpect(status().isNotFound());
  }

  private ResultActions zone() throws Exception {
    return mockMvc.perform(get("/api/v1/public/zones/" + plan.zoneId()));
  }

  private ResultActions register() throws Exception {
    String delivery =
        """
        {"deliveredAt": "2026-03-15T10:30:00",
         "items": [{"resourceId": "%s", "quantity": 300}]}
        """
            .formatted(plan.water());
    return mockMvc.perform(
        multipart("/api/v1/deliveries/" + deliveryId + "/registration")
            .file(
                new MockMultipartFile(
                    "delivery",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    delivery.getBytes(StandardCharsets.UTF_8)))
            .file(
                new MockMultipartFile(
                    "evidences",
                    "entrega.jpg",
                    "image/jpeg",
                    "foto".getBytes(StandardCharsets.UTF_8)))
            .with(plan.organization().fieldBrigade()));
  }
}
