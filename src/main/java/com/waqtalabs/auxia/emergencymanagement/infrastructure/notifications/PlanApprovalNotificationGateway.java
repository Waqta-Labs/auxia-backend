package com.waqtalabs.auxia.emergencymanagement.infrastructure.notifications;

import com.waqtalabs.auxia.emergencymanagement.application.ports.PlanApprovalNotifier;
import com.waqtalabs.auxia.identityaccess.interfaces.acl.IdentityAccessContextFacade;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailMessage;
import com.waqtalabs.auxia.shared.infrastructure.notifications.EmailSender;
import com.waqtalabs.auxia.shared.infrastructure.notifications.NotificationTexts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sends the approval of a plan by e-mail to the authority who approved it, as a confirmation, and
 * to the administrators of the organization, who prepare its resources.
 */
@Component
class PlanApprovalNotificationGateway implements PlanApprovalNotifier {

  private final IdentityAccessContextFacade identityAccess;
  private final EmailSender emailSender;
  private final NotificationTexts texts;

  PlanApprovalNotificationGateway(
      IdentityAccessContextFacade identityAccess,
      EmailSender emailSender,
      NotificationTexts texts) {
    this.identityAccess = identityAccess;
    this.emailSender = emailSender;
    this.texts = texts;
  }

  @Override
  public void notifyApproved(ApprovedPlan plan) {
    UUID approver = plan.approvedBy().value();
    Set<String> recipients =
        new HashSet<>(
            identityAccess.getActiveUserEmails(
                plan.organizationId().value(), "ORGANIZATION_ADMIN"));
    identityAccess.getActiveUserEmail(approver).ifPresent(recipients::add);
    if (recipients.isEmpty()) {
      return;
    }
    String approverName =
        identityAccess.getFullNames(List.of(approver)).getOrDefault(approver, "");
    emailSender.send(
        new EmailMessage(
            recipients,
            texts.get("notification.planApproved.subject", plan.zoneName()),
            texts.get(
                "notification.planApproved.body",
                plan.zoneName(),
                plan.emergencyName(),
                approverName,
                texts.get("notification.coverage." + plan.coverage().name()),
                plan.planId().value())));
  }
}
