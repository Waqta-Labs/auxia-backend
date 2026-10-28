package com.waqtalabs.auxia.traceability.interfaces.acl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public version of {@code DeliveryConfirmed}: a brigade delivered a plan and registered it with
 * its evidence. Resource Management consumes the stock and frees the brigade; Emergency
 * Management marks the plan executed and the zone served.
 *
 * @param items what was really delivered, which can be less than planned
 */
public record DeliveryConfirmedIntegrationEvent(
    UUID deliveryId,
    UUID distributionPlanId,
    UUID zoneId,
    UUID brigadeId,
    UUID organizationId,
    List<DeliveredItem> items,
    LocalDateTime deliveredAt) {

  /** Quantity of a resource that was delivered. */
  public record DeliveredItem(UUID resourceId, int quantity) {}
}
