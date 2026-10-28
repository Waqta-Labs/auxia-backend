package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CanonicalDeliveryRecordTests {

  private static final DeliveryId DELIVERY =
      new DeliveryId(UUID.fromString("0b6a3f52-9c1e-4d2b-8e7f-1a2b3c4d5e01"));
  private static final DistributionPlanId PLAN =
      new DistributionPlanId(UUID.fromString("1c7b4a63-0d2f-4e3c-9f80-2b3c4d5e6f02"));
  private static final ZoneId ZONE =
      new ZoneId(UUID.fromString("2d8c5b74-1e30-4f4d-a091-3c4d5e6f7a03"));
  private static final ResourceId WATER =
      new ResourceId(UUID.fromString("c7f1e2d3-4b5a-4c6d-8e9f-0a1b2c3d4e03"));
  private static final ResourceId FOOD =
      new ResourceId(UUID.fromString("d8a2f3e4-5c6b-4d7e-9f0a-1b2c3d4e5f04"));
  private static final HashValue PHOTO = new HashValue("a".repeat(64));
  private static final HashValue DOCUMENT = new HashValue("b".repeat(64));
  private static final LocalDateTime DELIVERED_AT = LocalDateTime.of(2026, 3, 15, 10, 30);

  @Test
  void jsonHasAFixedFormatWithTheDateToTheSecond() {
    var record =
        new CanonicalDeliveryRecord(
            DELIVERY,
            PLAN,
            ZONE,
            List.of(new CanonicalDeliveryRecord.Line(WATER, 300)),
            List.of(PHOTO),
            DELIVERED_AT);

    assertThat(record.toCanonicalJson())
        .isEqualTo(
            "{\"deliveryId\":\"0b6a3f52-9c1e-4d2b-8e7f-1a2b3c4d5e01\","
                + "\"distributionPlanId\":\"1c7b4a63-0d2f-4e3c-9f80-2b3c4d5e6f02\","
                + "\"zoneId\":\"2d8c5b74-1e30-4f4d-a091-3c4d5e6f7a03\","
                + "\"items\":[{\"resourceId\":\"c7f1e2d3-4b5a-4c6d-8e9f-0a1b2c3d4e03\","
                + "\"quantity\":300}],"
                + "\"evidenceHashes\":[\"" + PHOTO.value() + "\"],"
                + "\"deliveredAt\":\"2026-03-15T10:30:00\"}");
  }

  @Test
  void orderOfTheLinesAndFilesDoesNotChangeTheRecord() {
    var one =
        new CanonicalDeliveryRecord(
            DELIVERY,
            PLAN,
            ZONE,
            List.of(
                new CanonicalDeliveryRecord.Line(FOOD, 140),
                new CanonicalDeliveryRecord.Line(WATER, 300)),
            List.of(DOCUMENT, PHOTO),
            DELIVERED_AT);
    var other =
        new CanonicalDeliveryRecord(
            DELIVERY,
            PLAN,
            ZONE,
            List.of(
                new CanonicalDeliveryRecord.Line(WATER, 300),
                new CanonicalDeliveryRecord.Line(FOOD, 140)),
            List.of(PHOTO, DOCUMENT),
            DELIVERED_AT);

    assertThat(one.toCanonicalJson()).isEqualTo(other.toCanonicalJson());
  }

  @Test
  void recordCarriesOnlyIdentifiersQuantitiesHashesAndTheDate() {
    var record =
        new CanonicalDeliveryRecord(
            DELIVERY,
            PLAN,
            ZONE,
            List.of(new CanonicalDeliveryRecord.Line(WATER, 300)),
            List.of(PHOTO),
            DELIVERED_AT);

    Map<String, Object> fields = JsonPath.read(record.toCanonicalJson(), "$");
    assertThat(fields)
        .containsOnlyKeys(
            "deliveryId", "distributionPlanId", "zoneId", "items", "evidenceHashes", "deliveredAt");
    List<Map<String, Object>> items = JsonPath.read(record.toCanonicalJson(), "$.items");
    assertThat(items.getFirst()).containsOnlyKeys("resourceId", "quantity");
  }
}
