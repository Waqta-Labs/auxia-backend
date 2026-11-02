package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The record of a delivery whose hash goes to the blockchain (TS-C02, US-18).
 *
 * <p>It is built from a closed list of fields: the identifiers of the delivery, its plan and its
 * zone, the delivered quantities, the hashes of the evidence files and the delivery date. Names,
 * documents and any other data of the people who received the aid are not among them, so they
 * can never reach the hash. The same delivery always gives the same JSON: fixed field order,
 * lines ordered by resource, hashes ordered, and the date to the second.
 */
public record CanonicalDeliveryRecord(
    DeliveryId deliveryId,
    DistributionPlanId distributionPlanId,
    ZoneId zoneId,
    List<Line> items,
    List<HashValue> evidenceHashes,
    LocalDateTime deliveredAt) {

  private static final DateTimeFormatter SECONDS =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

  /** Quantity of a resource that was delivered. */
  public record Line(ResourceId resourceId, int quantity) {}

  public CanonicalDeliveryRecord {
    Objects.requireNonNull(deliveryId, "deliveryId");
    Objects.requireNonNull(distributionPlanId, "distributionPlanId");
    Objects.requireNonNull(zoneId, "zoneId");
    Objects.requireNonNull(deliveredAt, "deliveredAt");
    items =
        items.stream()
            .sorted(Comparator.comparing(line -> line.resourceId().value().toString()))
            .toList();
    evidenceHashes =
        evidenceHashes.stream().sorted(Comparator.comparing(HashValue::value)).toList();
  }

  /** Compact JSON. Every value is a UUID, a number, a hash or a date: none needs escaping. */
  public String toCanonicalJson() {
    String lines =
        items.stream()
            .map(
                line ->
                    "{\"resourceId\":\"%s\",\"quantity\":%d}"
                        .formatted(line.resourceId().value(), line.quantity()))
            .collect(Collectors.joining(","));
    String hashes =
        evidenceHashes.stream()
            .map(hash -> "\"" + hash.value() + "\"")
            .collect(Collectors.joining(","));
    return "{\"deliveryId\":\"" + deliveryId.value()
        + "\",\"distributionPlanId\":\"" + distributionPlanId.value()
        + "\",\"zoneId\":\"" + zoneId.value()
        + "\",\"items\":[" + lines
        + "],\"evidenceHashes\":[" + hashes
        + "],\"deliveredAt\":\"" + SECONDS.format(deliveredAt)
        + "\"}";
  }
}
