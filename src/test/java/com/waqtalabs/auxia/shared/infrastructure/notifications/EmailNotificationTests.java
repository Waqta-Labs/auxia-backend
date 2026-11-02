package com.waqtalabs.auxia.shared.infrastructure.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.FakeBlockchainConfiguration;
import com.waqtalabs.auxia.FakeEmailConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

/**
 * The e-mails of the Notification Service through the real flows: a low stock alert, an approved
 * plan and a certified delivery. Each one goes to the users whose role must act on it, in the
 * language of the notifications (Spanish).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  FakeBlockchainConfiguration.class,
  FakeEmailConfiguration.class
})
class EmailNotificationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;
  @Autowired private FakeEmailSender emails;

  @BeforeEach
  void resetFakes() {
    aiService.reset();
    ledger.reset();
  }

  @Test
  void lowStockIsSentToTheAdministrators() throws Exception {
    var organization = SignedInOrganization.register(mockMvc);
    String resource = "Agua " + UUID.randomUUID();
    String inventory =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/v1/inventories")
                        .with(organization.administrator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Almacén Lima\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");

    mockMvc
        .perform(
            post("/api/v1/inventories/" + inventory + "/resources")
                .with(organization.administrator())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "%s", "category": "WATER", "unit": "UNIT", "minimumStock": 50}
                    """
                        .formatted(resource)))
        .andExpect(status().isCreated());

    List<EmailMessage> sent = emails.withSubject("Stock bajo: " + resource);
    assertThat(sent).hasSize(1);
    assertThat(sent.getFirst().to()).containsExactly(email(organization, "ORGANIZATION_ADMIN"));
    assertThat(sent.getFirst().body())
        .contains(resource + " del inventario Almacén Lima bajó a 0")
        .contains("stock mínimo de 50");
  }

  @Test
  void approvedPlanIsConfirmedToItsAuthorityAndSentToTheAdministrators() throws Exception {
    String zone = "Sector " + UUID.randomUUID();

    var plan = ApprovedPlan.prepare(mockMvc, aiService, zone);

    List<EmailMessage> sent = emails.withSubject("Plan de distribución aprobado para " + zone);
    assertThat(sent).hasSize(1);
    assertThat(sent.getFirst().to())
        .containsExactlyInAnyOrder(
            email(plan.organization(), "ORGANIZATION_ADMIN"),
            email(plan.organization(), "AUTHORITY"));
    assertThat(sent.getFirst().body())
        .contains("Rosa Quispe aprobó el plan de distribución de la zona " + zone)
        .contains("emergencia Huaico Chosica 2026")
        .contains("Cobertura: total")
        .contains(plan.planId());
  }

  @Test
  void certifiedDeliveryIsSentToTheAuthoritiesWithItsTransaction() throws Exception {
    String zone = "Sector " + UUID.randomUUID();
    var plan = ApprovedPlan.prepare(mockMvc, aiService, zone);
    var organization = plan.organization();
    String deliveryId =
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

    mockMvc
        .perform(
            multipart("/api/v1/deliveries/" + deliveryId + "/registration")
                .file(
                    new MockMultipartFile(
                        "delivery",
                        "",
                        MediaType.APPLICATION_JSON_VALUE,
                        """
                        {"deliveredAt": "2026-03-15T10:30:00",
                         "items": [{"resourceId": "%s", "quantity": 300}]}
                        """
                            .formatted(plan.water())
                            .getBytes(StandardCharsets.UTF_8)))
                .file(
                    new MockMultipartFile(
                        "evidences",
                        "entrega.jpg",
                        "image/jpeg",
                        "foto".getBytes(StandardCharsets.UTF_8)))
                .with(organization.fieldBrigade()))
        .andExpect(status().isCreated());
    String transaction =
        JsonPath.read(
            mockMvc
                .perform(get("/api/v1/deliveries/" + deliveryId).with(organization.auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.integrity.transactionHash");

    List<EmailMessage> sent = emails.withSubject("Entrega certificada en " + zone);
    assertThat(sent).hasSize(1);
    assertThat(sent.getFirst().to()).containsExactly(email(organization, "AUTHORITY"));
    assertThat(sent.getFirst().body())
        .contains("realizada el 2026-03-15 10:30, quedó certificada")
        .contains("Entrega: " + deliveryId)
        .contains("https://sepolia.etherscan.io/tx/" + transaction);
  }

  /** E-mail of the only user of the organization with the role. */
  private String email(SignedInOrganization organization, String role) throws Exception {
    List<String> emailsOfRole =
        JsonPath.read(
            mockMvc
                .perform(
                    get("/api/v1/organizations/" + organization.organizationId() + "/users")
                        .with(organization.administrator()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[?(@.role == '" + role + "')].email");
    assertThat(emailsOfRole).hasSize(1);
    return emailsOfRole.getFirst();
  }
}
