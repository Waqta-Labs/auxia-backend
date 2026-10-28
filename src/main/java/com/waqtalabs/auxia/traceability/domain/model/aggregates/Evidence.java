package com.waqtalabs.auxia.traceability.domain.model.aggregates;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Photo or document that backs a delivery. Entity inside the {@link Delivery} aggregate. The file
 * lives in the evidence storage; its hash is part of the record of the delivery, so changing the
 * file after the registration is also detected.
 */
public final class Evidence {

  private final EvidenceId id;
  private final EvidenceType type;
  private final String storageUrl;
  private final HashValue contentHash;
  private final LocalDateTime capturedAt;
  private final GeoPoint location;

  private Evidence(
      EvidenceId id,
      EvidenceType type,
      String storageUrl,
      HashValue contentHash,
      LocalDateTime capturedAt,
      GeoPoint location) {
    this.id = Objects.requireNonNull(id, "id");
    this.type = Objects.requireNonNull(type, "type");
    Objects.requireNonNull(storageUrl, "storageUrl");
    if (storageUrl.isBlank()) {
      throw new IllegalArgumentException("storageUrl must not be blank");
    }
    this.storageUrl = storageUrl;
    this.contentHash = Objects.requireNonNull(contentHash, "contentHash");
    this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    this.location = location;
  }

  /**
   * An evidence whose file is already stored.
   *
   * @param location where it was captured, or {@code null} if unknown
   */
  public static Evidence attach(
      EvidenceType type,
      String storageUrl,
      HashValue contentHash,
      LocalDateTime capturedAt,
      GeoPoint location) {
    return new Evidence(
        EvidenceId.generate(), type, storageUrl, contentHash, capturedAt, location);
  }

  public static Evidence reconstitute(
      EvidenceId id,
      EvidenceType type,
      String storageUrl,
      HashValue contentHash,
      LocalDateTime capturedAt,
      GeoPoint location) {
    return new Evidence(id, type, storageUrl, contentHash, capturedAt, location);
  }

  public EvidenceId id() {
    return id;
  }

  public EvidenceType type() {
    return type;
  }

  public String storageUrl() {
    return storageUrl;
  }

  public HashValue contentHash() {
    return contentHash;
  }

  public LocalDateTime capturedAt() {
    return capturedAt;
  }

  public Optional<GeoPoint> location() {
    return Optional.ofNullable(location);
  }
}
