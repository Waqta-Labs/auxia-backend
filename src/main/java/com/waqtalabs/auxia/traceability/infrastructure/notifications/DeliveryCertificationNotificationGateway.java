package com.waqtalabs.auxia.traceability.infrastructure.notifications;

import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailSender;
import com.waqtalabs.auxia.shared.infrastructure.notifications.NotificationTexts;
import com.waqtalabs.auxia.traceability.application.ports.DeliveryCertificationNotifier;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends a certified delivery by e-mail to the authorities of the organization, with the link to
 * its transaction in the block explorer.
 */
@Component
class DeliveryCertificationNotificationGateway implements DeliveryCertificationNotifier {

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final IdentityAccessContextFacade identityAccess;
  private final EmailSender emailSender;
  private final NotificationTexts texts;
  private final String explorerUrl;

  DeliveryCertificationNotificationGateway(
      IdentityAccessContextFacade identityAccess,
      EmailSender emailSender,
      NotificationTexts texts,
      @Value("${auxia.blockchain.explorer-url}") String explorerUrl) {
    this.identityAccess = identityAccess;
    this.emailSender = emailSender;
    this.texts = texts;
    this.explorerUrl = explorerUrl;
  }

  @Override
  public void notifyCertified(CertifiedDelivery delivery) {
    List<String> authorities =
        identityAccess.getActiveUserEmails(delivery.organizationId().value(), "AUTHORITY");
    if (authorities.isEmpty()) {
      return;
    }
    emailSender.send(
        new EmailMessage(
            new HashSet<>(authorities),
            texts.get("notification.deliveryCertified.subject", delivery.zoneName()),
            texts.get(
                "notification.deliveryCertified.body",
                delivery.zoneName(),
                DATE_TIME.format(delivery.deliveredAt()),
                delivery.deliveryId().value(),
                explorerUrl + "/tx/" + delivery.transactionHash())));
  }
}
