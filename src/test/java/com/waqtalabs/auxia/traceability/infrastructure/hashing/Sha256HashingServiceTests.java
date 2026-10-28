package com.waqtalabs.auxia.traceability.infrastructure.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Sha256HashingServiceTests {

  private final Sha256HashingService hashing = new Sha256HashingService();

  @Test
  void contentHashIsTheStandardSha256() {
    assertThat(hashing.hashContent("abc".getBytes(StandardCharsets.UTF_8)))
        .isEqualTo(
            new HashValue("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
    assertThat(hashing.hashContent(new byte[0]))
        .isEqualTo(
            new HashValue("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
  }

  @Test
  void recordHashIsTheHashOfItsCanonicalJson() {
    var record =
        new CanonicalDeliveryRecord(
            new DeliveryId(UUID.randomUUID()),
            new DistributionPlanId(UUID.randomUUID()),
            new ZoneId(UUID.randomUUID()),
            List.of(new CanonicalDeliveryRecord.Line(new ResourceId(UUID.randomUUID()), 300)),
            List.of(new HashValue("a".repeat(64))),
            LocalDateTime.of(2026, 3, 15, 10, 30));

    assertThat(hashing.hashRecord(record))
        .isEqualTo(
            hashing.hashContent(record.toCanonicalJson().getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void anyChangeOfAQuantityChangesTheHash() {
    var delivery = new DeliveryId(UUID.randomUUID());
    var plan = new DistributionPlanId(UUID.randomUUID());
    var zone = new ZoneId(UUID.randomUUID());
    var water = new ResourceId(UUID.randomUUID());
    var at = LocalDateTime.of(2026, 3, 15, 10, 30);
    var photo = List.of(new HashValue("a".repeat(64)));

    HashValue original =
        hashing.hashRecord(
            new CanonicalDeliveryRecord(
                delivery,
                plan,
                zone,
                List.of(new CanonicalDeliveryRecord.Line(water, 100)),
                photo,
                at));
    HashValue altered =
        hashing.hashRecord(
            new CanonicalDeliveryRecord(
                delivery,
                plan,
                zone,
                List.of(new CanonicalDeliveryRecord.Line(water, 900)),
                photo,
                at));

    assertThat(altered).isNotEqualTo(original);
  }
}
