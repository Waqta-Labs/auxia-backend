package com.waqtalabs.auxia.traceability.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.FakeBlockchainConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Registration, anchoring and verification of a delivery through the API (US-15 to US-18,
 * TS-05). The anchoring runs in the request thread and the contract is {@link
 * FakeBlockchainLedger}; the evidence files go to Azurite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  FakeBlockchainConfiguration.class
})
class DeliveryControllerTests {

  private static final byte[] PHOTO = "foto de la entrega".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ACTA = "%PDF acta firmada".getBytes(StandardCharsets.UTF_8);

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;
  @Autowired private JdbcTemplate jdbc;

  private ApprovedPlan plan;
  private SignedInOrganization organization;
  private String deliveryId;

  @BeforeEach
  void approvedPlanWithItsDeliveryOpen() throws Exception {
    aiService.reset();
    ledger.reset();
    plan = ApprovedPlan.prepare(mockMvc, aiService);
    organization = plan.organization();
    deliveryId =
        JsonPath.read(
            mockMvc
                .perform(
                    get("/api/v1/deliveries")
                        .param("zoneId", plan.zoneId())
                        .with(organization.auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
  }

  @Test
  void assignedBrigadeOpensTheDeliveryOfThePlan() throws Exception {
    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId).with(organization.auditor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.distributionPlanId").value(plan.planId()))
        .andExpect(jsonPath("$.brigadeId").value(plan.brigadeId()))
        .andExpect(jsonPath("$.status").value("INITIATED"))
        .andExpect(jsonPath("$.integrity").value(nullValue()));
  }

  @Test
  void registeredDeliveryIsAnchoredAndReleasesItsStockAndBrigade() throws Exception {
    register(organization.fieldBrigade(), delivery(plan.water(), 300), photo(), acta())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("REGISTERED"))
        .andExpect(jsonPath("$.deliveredAt").value("2026-03-15T10:30:00"))
        .andExpect(jsonPath("$.items[0].quantity").value(300))
        .andExpect(jsonPath("$.evidences[*].type").value(contains("PHOTO", "DOCUMENT")))
        .andExpect(jsonPath("$.evidences[0].location.latitude").value(-11.9436))
        .andExpect(jsonPath("$.integrity.recordHash").value(matchesPattern("[0-9a-f]{64}")));

    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId).with(organization.auditor()))
        .andExpect(jsonPath("$.status").value("CERTIFIED"))
        .andExpect(jsonPath("$.integrity.anchorStatus").value("ANCHORED"))
        .andExpect(jsonPath("$.integrity.transactionHash").value(matchesPattern("0x[0-9a-f]{64}")))
        .andExpect(jsonPath("$.integrity.attempts").value(1));
    mockMvc
        .perform(get("/api/v1/inventories").with(organization.administrator()))
        .andExpect(jsonPath("$.stockItems[*].onHand").value(contains(200, 300)))
        .andExpect(jsonPath("$.stockItems[*].reserved").value(contains(0, 0)));
    mockMvc
        .perform(get("/api/v1/brigades/" + plan.brigadeId()).with(organization.administrator()))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));
    mockMvc
        .perform(
            get("/api/v1/distribution-plans/" + plan.planId()).with(organization.auditor()))
        .andExpect(jsonPath("$.status").value("EXECUTED"));
    mockMvc
        .perform(get("/api/v1/zones/" + plan.zoneId()).with(organization.auditor()))
        .andExpect(jsonPath("$.status").value("SERVED"))
        .andExpect(jsonPath("$.lastAidReceivedAt").value("2026-03-15T10:30:00"));
  }

  @Test
  void anyoneVerifiesADeliveryAndAlteredDataIsDetected() throws Exception {
    register(organization.fieldBrigade(), delivery(plan.water(), 100), photo())
        .andExpect(status().isCreated());

    String recordHash =
        JsonPath.read(
            mockMvc
                .perform(get("/api/v1/deliveries/" + deliveryId + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.recordHash");
    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId + "/verify"))
        .andExpect(jsonPath("$.anchoredHash").value(recordHash))
        .andExpect(jsonPath("$.transactionHash").value(matchesPattern("0x[0-9a-f]{64}")));

    jdbc.update(
        "UPDATE traceability.delivery_items SET quantity = 900 WHERE delivery_id = ?",
        UUID.fromString(deliveryId));

    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId + "/verify"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verified").value(false))
        .andExpect(jsonPath("$.anchoredHash").value(recordHash));
  }

  @Test
  void registrationSentAgainReturnsTheSameDelivery() throws Exception {
    String first =
        JsonPath.read(
            register(organization.fieldBrigade(), delivery(plan.water(), 300), photo())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.integrity.recordHash");

    register(organization.fieldBrigade(), delivery(plan.water(), 300), photo())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.integrity.recordHash").value(first));
    assertThat(ledger.sentTransactions()).hasSize(1);
  }

  @Test
  void registrationWithoutEvidenceIsRejectedAndComputesNoHash() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/deliveries/" + deliveryId + "/registration")
                .file(deliveryPart(delivery(plan.water(), 300)))
                .with(organization.fieldBrigade()))
        .andExpect(status().isBadRequest());
    register(
            organization.fieldBrigade(),
            delivery(plan.water(), 300),
            new MockMultipartFile("evidences", "vacia.jpg", "image/jpeg", new byte[0]))
        .andExpect(status().isUnprocessableContent());

    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId).with(organization.auditor()))
        .andExpect(jsonPath("$.status").value("INITIATED"))
        .andExpect(jsonPath("$.integrity").value(nullValue()));
  }

  @Test
  void deliveryMustFitThePlanAndCarryPhotosOrDocuments() throws Exception {
    register(organization.fieldBrigade(), delivery(plan.water(), 501), photo())
        .andExpect(status().isUnprocessableContent());
    register(organization.fieldBrigade(), delivery(UUID.randomUUID().toString(), 1), photo())
        .andExpect(status().isUnprocessableContent());
    register(
            organization.fieldBrigade(),
            delivery(plan.water(), 300),
            new MockMultipartFile("evidences", "nota.txt", "text/plain", PHOTO))
        .andExpect(status().isUnprocessableContent());
    register(organization.fieldBrigade(), "{\"items\": []}", photo())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasSize(2)));
  }

  @Test
  void onlyTheBrigadeOrAnAuthorityOfTheOrganizationRegisters() throws Exception {
    register(organization.auditor(), delivery(plan.water(), 300), photo())
        .andExpect(status().isForbidden());
    var other = SignedInOrganization.register(mockMvc);
    register(other.fieldBrigade(), delivery(plan.water(), 300), photo())
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId).with(other.auditor()))
        .andExpect(status().isNotFound());
    register(organization.authority(), delivery(plan.water(), 300), photo())
        .andExpect(status().isCreated());
  }

  @Test
  void blockchainDownLeavesTheDeliveryRegisteredForARetry() throws Exception {
    ledger.becomeUnavailable();

    register(organization.fieldBrigade(), delivery(plan.water(), 300), photo())
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId).with(organization.auditor()))
        .andExpect(jsonPath("$.status").value("REGISTERED"))
        .andExpect(jsonPath("$.integrity.anchorStatus").value("FAILED"));
    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId + "/verify"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void verificationOfAnUnknownOrOpenDeliveryIsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/deliveries/" + UUID.randomUUID() + "/verify"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId + "/verify"))
        .andExpect(status().isNotFound());
  }

  private ResultActions register(
      RequestPostProcessor user, String delivery, MockMultipartFile... evidences)
      throws Exception {
    MockMultipartHttpServletRequestBuilder request =
        multipart("/api/v1/deliveries/" + deliveryId + "/registration");
    request.file(deliveryPart(delivery));
    for (MockMultipartFile evidence : evidences) {
      request.file(evidence);
    }
    return mockMvc.perform(request.with(user));
  }

  private static MockMultipartFile deliveryPart(String json) {
    return new MockMultipartFile(
        "delivery", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
  }

  private static String delivery(String resourceId, int quantity) {
    return """
        {
          "deliveredAt": "2026-03-15T10:30:00",
          "items": [{"resourceId": "%s", "quantity": %d}],
          "location": {"latitude": -11.9436, "longitude": -76.7078}
        }
        """
        .formatted(resourceId, quantity);
  }

  private static MockMultipartFile photo() {
    return new MockMultipartFile("evidences", "entrega.jpg", "image/jpeg", PHOTO);
  }

  private static MockMultipartFile acta() {
    return new MockMultipartFile("evidences", "acta.pdf", "application/pdf", ACTA);
  }
}
