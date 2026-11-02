package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/**
 * Confirms an approved plan to the authority who approved it and tells the administrators, who
 * prepare its resources (Notification Service).
 */
@FunctionalInterface
public interface PlanApprovalNotifier {

  void notifyApproved(ApprovedPlan plan);

  /** What the notification says about the plan. */
  record ApprovedPlan(
      DistributionPlanId planId,
      OrganizationId organizationId,
      AuthorityId approvedBy,
      String emergencyName,
      String zoneName,
      Coverage coverage) {}
}
