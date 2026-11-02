package com.waqtalabs.auxia.resourcemanagement.infrastructure.notifications;

import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import com.waqtalabs.auxia.resourcemanagement.application.ports.StockAlertNotifier;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailSender;
import com.waqtalabs.auxia.shared.infrastructure.notifications.NotificationTexts;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends each new low stock alert by e-mail to the administrators of the organization, who manage
 * the inventory (US-10). The alerts are also read with {@code GET /inventories/{id}/alerts}.
 */
@Component
class NotificationServiceGateway implements StockAlertNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceGateway.class);

  private final InventoryRepository inventoryRepository;
  private final IdentityAccessContextFacade identityAccess;
  private final EmailSender emailSender;
  private final NotificationTexts texts;

  NotificationServiceGateway(
      InventoryRepository inventoryRepository,
      IdentityAccessContextFacade identityAccess,
      EmailSender emailSender,
      NotificationTexts texts) {
    this.inventoryRepository = inventoryRepository;
    this.identityAccess = identityAccess;
    this.emailSender = emailSender;
    this.texts = texts;
  }

  @Override
  public void notify(StockAlert alert) {
    Inventory inventory = inventoryRepository.findById(alert.inventoryId()).orElse(null);
    if (inventory == null) {
      return;
    }
    List<String> administrators =
        identityAccess.getActiveUserEmails(
            inventory.organizationId().value(), "ORGANIZATION_ADMIN");
    if (administrators.isEmpty()) {
      LOGGER.warn("Low stock of {} has nobody to notify", alert.resourceName());
      return;
    }
    emailSender.send(
        new EmailMessage(
            new HashSet<>(administrators),
            texts.get("notification.stockAlert.subject", alert.resourceName()),
            texts.get(
                "notification.stockAlert.body",
                alert.resourceName(),
                inventory.name(),
                alert.availableQuantity(),
                alert.minimumStock())));
  }
}
