package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Steps of US-24 to US-26. The zone gets its plan, brigade and delivery through the API, as
 * authorities and brigades do; the citizen queries without a token.
 */
public class PublicStatusSteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;

  private ApprovedPlan plan;
  private String deliveryId;
  private MockHttpServletResponse response;

  @Before
  public void resetFakes() {
    aiService.reset();
    ledger.reset();
  }

  @Dado("que la zona {string} tiene un plan de ayuda aprobado y su brigada en camino")
  public void aZoneWithItsAidOnTheWay(String zone) throws Exception {
    plan = ApprovedPlan.prepare(mockMvc, aiService, zone);
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

  @Dado("que la brigada registró la entrega del {string} y quedó verificada en Blockchain")
  public void theDeliveryWasRegisteredAndVerified(String deliveredAt) throws Exception {
    register(deliveredAt);
  }

  @Dado(
      "que la brigada registró la entrega del {string} y todavía no está verificada en"
          + " Blockchain")
  public void theDeliveryWasRegisteredButNotVerified(String deliveredAt) throws Exception {
    ledger.becomeUnavailable();
    register(deliveredAt);
    ledger.becomeAvailable();
  }

  @Cuando("un ciudadano consulta el estado de la zona sin iniciar sesión")
  public void aCitizenQueriesTheZone() throws Exception {
    response = publicGet("/api/v1/public/zones/" + plan.zoneId());
  }

  @Cuando("un ciudadano consulta el estado de una zona que no está registrada")
  public void aCitizenQueriesAnUnknownZone() throws Exception {
    response = publicGet("/api/v1/public/zones/" + UUID.randomUUID());
  }

  @Cuando("un ciudadano ingresa el código público de la entrega")
  public void aCitizenEntersTheCode() throws Exception {
    String zone = publicGet("/api/v1/public/zones/" + plan.zoneId()).getContentAsString();
    response = publicGet(JsonPath.read(zone, "$.verificationUrl"));
  }

  @Cuando("un ciudadano ingresa un código que no corresponde a ninguna entrega")
  public void aCitizenEntersAnUnknownCode() throws Exception {
    response = publicGet("/api/v1/deliveries/" + UUID.randomUUID() + "/verify");
  }

  @Entonces("se muestra la etapa {string}")
  public void theStageIsShown(String stage) throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.stage")).isEqualTo(stage);
  }

  @Y("no se muestra el score de urgencia ni información de terceros")
  public void noScoreNorThirdPartyData() throws Exception {
    Map<String, Object> zone = JsonPath.read(response.getContentAsString(), "$");
    assertThat(zone)
        .containsOnlyKeys(
            "zoneId",
            "emergencyId",
            "zoneName",
            "stage",
            "deliveriesCompleted",
            "lastDeliveryAt",
            "lastDeliveryVerification",
            "verificationUrl");
  }

  @Entonces("el sistema indica que no existe información disponible")
  public void thereIsNoInformation() throws Exception {
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.detail"))
        .contains("no information available");
  }

  @Entonces(
      "se muestra {int} entrega(s) realizada(s), la última del {string}, con el estado {string}")
  public void theHistoryIsShown(int deliveries, String lastDeliveryAt, String verification)
      throws Exception {
    String zone = response.getContentAsString();
    assertThat(JsonPath.<Integer>read(zone, "$.deliveriesCompleted")).isEqualTo(deliveries);
    assertThat(JsonPath.<String>read(zone, "$.lastDeliveryAt")).isEqualTo(lastDeliveryAt);
    assertThat(JsonPath.<String>read(zone, "$.lastDeliveryVerification")).isEqualTo(verification);
  }

  @Y("no se muestran datos personales ni los recursos entregados")
  public void noPersonalDataNorResources() throws Exception {
    noScoreNorThirdPartyData();
    assertThat(response.getContentAsString()).doesNotContain(plan.water());
  }

  @Entonces("la última entrega se muestra con el estado {string}")
  public void theLastDeliveryShows(String verification) throws Exception {
    assertThat(
            JsonPath.<String>read(response.getContentAsString(), "$.lastDeliveryVerification"))
        .isEqualTo(verification);
  }

  @Entonces("el sistema indica que la entrega es válida")
  public void theDeliveryIsValid() throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.deliveryId"))
        .isEqualTo(deliveryId);
    assertThat(JsonPath.<Boolean>read(response.getContentAsString(), "$.verified")).isTrue();
  }

  @Entonces("el sistema indica que el código no es válido")
  public void theCodeIsNotValid() {
    assertThat(response.getStatus()).isEqualTo(404);
  }

  private MockHttpServletResponse publicGet(String path) throws Exception {
    return mockMvc.perform(get(path)).andReturn().getResponse();
  }

  private void register(String deliveredAt) throws Exception {
    String delivery =
        """
        {"deliveredAt": "%s", "items": [{"resourceId": "%s", "quantity": 300}]}
        """
            .formatted(deliveredAt, plan.water());
    int status =
        mockMvc
            .perform(
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
                            "foto de la entrega".getBytes(StandardCharsets.UTF_8)))
                    .with(plan.organization().fieldBrigade()))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(201);
  }
}
