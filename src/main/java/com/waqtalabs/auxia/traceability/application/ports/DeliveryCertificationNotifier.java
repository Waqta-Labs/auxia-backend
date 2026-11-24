package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import java.time.LocalDateTime;

/**
 * Tells the authorities of the organization that a delivery is certified, with the transaction
 * that proves it (Notification Service).
 */
@FunctionalInterface
public interface DeliveryCertificationNotifier {

  void notifyCertified(CertifiedDelivery delivery);

  /** What the notification says about the delivery. */
  record CertifiedDelivery(
      DeliveryId deliveryId,
      OrganizationId organizationId,
      String zoneName,
      LocalDateTime deliveredAt,
      String transactionHash) {}
}
