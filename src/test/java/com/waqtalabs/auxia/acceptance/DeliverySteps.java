package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.ApprovedPlan;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.traceability.application.FakeBlockchainLedger;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Steps of US-15 to US-18 and TS-05. Each scenario approves a plan through the API, which assigns
 * a brigade and opens the delivery. The brigade member signs in; the evidence files go to
 * Azurite, and the contract is the fake ledger, which anchors in the same request.
 */
public class DeliverySteps {

  private static final byte[] PHOTO = "foto de la entrega".getBytes(StandardCharsets.UTF_8);
  private static final String DELIVERED_AT = "2026-03-15T10:30:00";

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private FakeBlockchainLedger ledger;
  @Autowired private BlobContainerClient evidenceContainer;
  @Autowired private JdbcTemplate jdbc;

  private ApprovedPlan plan;
  private String deliveryId;
  private MockHttpServletResponse response;

  @Before
  public void resetFakes() {
    aiService.reset();
    ledger.reset();
  }

  @Dado("una distribución aprobada para la zona {string} con su brigada asignada")
  public void anApprovedPlanWithItsBrigade(String zone) throws Exception {
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

  /** Coordinates as text: the Spanish locale of the features would read the dot as grouping. */
  @Cuando(
      "la brigada registra la entrega de {int} unidades de agua con una foto tomada en la latitud"
          + " {string} y la longitud {string}")
  public void theBrigadeRegistersWithAPhotoTakenAt(int quantity, String latitude, String longitude)
      throws Exception {
    register(
        deliveryJson(
            quantity, Double.parseDouble(latitude), Double.parseDouble(longitude), ""),
        photo());
  }

  @Cuando("la brigada registra la entrega con una foto")
  @Dado("que la brigada registró la entrega con una foto")
  public void theBrigadeRegistersWithAPhoto() throws Exception {
    register(deliveryJson(300, -11.9436, -76.7078, ""), photo());
    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Cuando("la brigada registra la entrega con una foto y el campo {string} con {string}")
  public void theBrigadeRegistersWithAPersonalField(String field, String value) throws Exception {
    String personalField = ", \"%s\": \"%s\"".formatted(field, value);
    register(deliveryJson(300, -11.9436, -76.7078, personalField), photo());
    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Cuando("la brigada intenta registrar la entrega sin evidencia")
  @Cuando("se envía la solicitud POST de registro con la entrega y sin evidencia")
  public void theBrigadeRegistersWithoutEvidence() throws Exception {
    register(deliveryJson(300, -11.9436, -76.7078, ""));
  }

  @Cuando("se envía la solicitud POST de registro con la entrega y una foto")
  public void aRegistrationRequestWithAPhoto() throws Exception {
    register(deliveryJson(300, -11.9436, -76.7078, ""), photo());
  }

  @Cuando("alguien reemplaza la foto en el almacenamiento de evidencias")
  public void someoneReplacesThePhoto() throws Exception {
    String photoHash = JsonPath.read(delivery(), "$.evidences[0].contentHash");
    evidenceContainer
        .getBlobClient(deliveryId + "/" + photoHash)
        .upload(BinaryData.fromString("otra foto"), true);
  }

  @Dado("que alguien cambia en la base de datos la cantidad entregada a {int}")
  public void someoneChangesTheDeliveredQuantity(int quantity) {
    jdbc.update(
        "UPDATE traceability.delivery_items SET quantity = ? WHERE delivery_id = ?",
        quantity,
        UUID.fromString(deliveryId));
  }

  @Cuando("se verifica la entrega con su código, sin iniciar sesión")
  public void someoneVerifiesTheDelivery() throws Exception {
    response =
        mockMvc
            .perform(get("/api/v1/deliveries/" + deliveryId + "/verify"))
            .andReturn()
            .getResponse();
  }

  @Entonces("la evidencia queda asociada a la entrega y a la zona {string}")
  public void theEvidenceBelongsToTheDeliveryAndZone(String zone) throws Exception {
    assertThat(response.getStatus()).isEqualTo(201);
    String delivery = delivery();
    assertThat(JsonPath.<String>read(delivery, "$.zoneId")).isEqualTo(plan.zoneId());
    assertThat(JsonPath.<List<String>>read(delivery, "$.evidences[*].type"))
        .containsExactly("PHOTO");
    String zoneName =
        JsonPath.read(
            mockMvc
                .perform(
                    get("/api/v1/zones/" + plan.zoneId()).with(plan.organization().auditor()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.name");
    assertThat(zoneName).isEqualTo(zone);
  }

  @Y("la foto conserva la ubicación donde se tomó")
  public void thePhotoKeepsItsLocation() throws Exception {
    Map<String, Double> location = JsonPath.read(delivery(), "$.evidences[0].location");
    assertThat(location).containsEntry("latitude", -11.9436).containsEntry("longitude", -76.7078);
  }

  @Entonces("el sistema no permite completar el registro")
  public void theRegistrationIsRejected() {
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Y("la entrega sigue pendiente de registro")
  public void theDeliveryIsStillOpen() throws Exception {
    assertThat(JsonPath.<String>read(delivery(), "$.status")).isEqualTo("INITIATED");
  }

  @Entonces("se genera un hash de la entrega y de su evidencia")
  public void theDeliveryAndItsEvidenceAreHashed() throws Exception {
    String delivery = delivery();
    assertThat(JsonPath.<String>read(delivery, "$.integrity.recordHash")).matches("[0-9a-f]{64}");
    assertThat(JsonPath.<String>read(delivery, "$.evidences[0].contentHash"))
        .isEqualTo(sha256(PHOTO));
  }

  @Y("el hash queda almacenado en Blockchain con la transacción que lo registró")
  public void theHashIsInTheBlockchain() throws Exception {
    String delivery = delivery();
    assertThat(JsonPath.<String>read(delivery, "$.status")).isEqualTo("CERTIFIED");
    assertThat(JsonPath.<String>read(delivery, "$.integrity.transactionHash"))
        .isEqualTo(ledger.sentTransactions().getFirst());
    assertThat(ledger.deliveryHash(new DeliveryId(UUID.fromString(deliveryId))))
        .contains(new HashValue(JsonPath.read(delivery, "$.integrity.recordHash")));
  }

  @Entonces("el hash recalculado no coincide con el almacenado en Blockchain")
  public void theRecomputedHashDoesNotMatch() throws Exception {
    someoneVerifiesTheDelivery();
    theDeliveryIsNotVerified();
  }

  @Entonces("el sistema señala la entrega como no verificable")
  public void theDeliveryIsNotVerified() throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    String verification = response.getContentAsString();
    assertThat(JsonPath.<Boolean>read(verification, "$.verified")).isFalse();
    assertThat(JsonPath.<String>read(verification, "$.recordHash"))
        .isNotEqualTo(JsonPath.read(verification, "$.anchoredHash"));
  }

  @Entonces("el sistema indica que el hash actual coincide con el almacenado en Blockchain")
  public void theDeliveryIsVerified() throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    String verification = response.getContentAsString();
    assertThat(JsonPath.<Boolean>read(verification, "$.verified")).isTrue();
    assertThat(JsonPath.<String>read(verification, "$.recordHash"))
        .isEqualTo(JsonPath.read(verification, "$.anchoredHash"));
  }

  /**
   * The anchored hash is the SHA-256 of a record built only from these fields of the delivery.
   * Any other data, like the name of a person, would change it.
   */
  @Entonces(
      "el hash enviado a Blockchain solo cubre identificadores, cantidades, hashes de la evidencia"
          + " y la fecha")
  public void theAnchoredHashCoversOnlyNonSensitiveData() throws Exception {
    String delivery = delivery();
    List<Map<String, Object>> items = JsonPath.read(delivery, "$.items");
    List<String> evidenceHashes = JsonPath.read(delivery, "$.evidences[*].contentHash");
    String hashes =
        evidenceHashes.stream()
            .sorted()
            .map(hash -> "\"" + hash + "\"")
            .collect(Collectors.joining(","));
    String record =
        "{\"deliveryId\":\"" + deliveryId
            + "\",\"distributionPlanId\":\"" + plan.planId()
            + "\",\"zoneId\":\"" + plan.zoneId()
            + "\",\"items\":[" + lines(items)
            + "],\"evidenceHashes\":[" + hashes
            + "],\"deliveredAt\":\"" + DELIVERED_AT
            + "\"}";
    String expected = sha256(record.getBytes(StandardCharsets.UTF_8));
    assertThat(JsonPath.<String>read(delivery, "$.integrity.recordHash")).isEqualTo(expected);
    assertThat(ledger.deliveryHash(new DeliveryId(UUID.fromString(deliveryId))))
        .contains(new HashValue(expected));
  }

  @Entonces("la entrega no guarda {string}")
  public void theDeliveryDoesNotKeep(String value) throws Exception {
    assertThat(response.getContentAsString()).doesNotContain(value);
    assertThat(delivery()).doesNotContain(value);
  }

  @Entonces("la respuesta tiene el código {int} y el hash generado")
  public void theResponseHasTheHash(int status) throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.getContentAsString(), "$.integrity.recordHash"))
        .matches("[0-9a-f]{64}");
  }

  @Y("la entrega muestra la referencia de la transacción en Blockchain")
  public void theDeliveryShowsItsTransaction() throws Exception {
    assertThat(JsonPath.<String>read(delivery(), "$.integrity.transactionHash"))
        .matches("0x[0-9a-f]{64}");
  }

  @Entonces("la respuesta tiene el código {int}")
  public void theResponseHasTheStatus(int status) {
    assertThat(response.getStatus()).isEqualTo(status);
  }

  @Y("la entrega no tiene un hash generado")
  public void theDeliveryHasNoHash() throws Exception {
    assertThat(JsonPath.<Object>read(delivery(), "$.integrity")).isNull();
  }

  private void register(String delivery, MockMultipartFile... evidences) throws Exception {
    MockMultipartHttpServletRequestBuilder request =
        multipart("/api/v1/deliveries/" + deliveryId + "/registration");
    request.file(
        new MockMultipartFile(
            "delivery",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            delivery.getBytes(StandardCharsets.UTF_8)));
    for (MockMultipartFile evidence : evidences) {
      request.file(evidence);
    }
    response =
        mockMvc.perform(request.with(plan.organization().fieldBrigade())).andReturn().getResponse();
  }

  private String delivery() throws Exception {
    return mockMvc
        .perform(get("/api/v1/deliveries/" + deliveryId).with(plan.organization().auditor()))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String deliveryJson(
      int quantity, double latitude, double longitude, String extraFields) {
    return """
        {
          "deliveredAt": "%s",
          "items": [{"resourceId": "%s", "quantity": %d}],
          "location": {"latitude": %s, "longitude": %s}%s
        }
        """
        .formatted(DELIVERED_AT, plan.water(), quantity, latitude, longitude, extraFields);
  }

  private static MockMultipartFile photo() {
    return new MockMultipartFile("evidences", "entrega.jpg", "image/jpeg", PHOTO);
  }

  private static String lines(List<Map<String, Object>> items) {
    return items.stream()
        .sorted(Comparator.comparing(item -> item.get("resourceId").toString()))
        .map(
            item ->
                "{\"resourceId\":\"%s\",\"quantity\":%s}"
                    .formatted(item.get("resourceId"), item.get("quantity")))
        .collect(Collectors.joining(","));
  }

  private static String sha256(byte[] content) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
  }
}
