package com.waqtalabs.auxia.traceability.application.commands;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Registers what a brigade delivered, with its evidence files (US-15, TS-05).
 *
 * @param location where the evidence was captured; empty if the device had no location
 */
public record RegisterDeliveryCommand(
    DeliveryId deliveryId,
    OrganizationId organizationId,
    UserId registeredBy,
    LocalDateTime deliveredAt,
    List<DeliveredQuantity> items,
    Optional<GeoPoint> location,
    List<EvidenceFile> evidences) {

  /** Quantity of a resource that was delivered. */
  public record DeliveredQuantity(ResourceId resourceId, int quantity) {}

  /**
   * A file that backs the delivery, as it arrived.
   *
   * @param contentType media type of the file, such as {@code image/jpeg}
   */
  public record EvidenceFile(String contentType, byte[] content) {}
}
