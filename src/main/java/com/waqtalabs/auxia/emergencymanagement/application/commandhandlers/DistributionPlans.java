package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.shared.application.exceptions.ActionNotAllowedException;

/** Rules shared by the handlers in which an authority decides a distribution plan. */
final class DistributionPlans {

  private DistributionPlans() {}

  /**
   * Confirms that the user is still an active authority, because their role may have changed
   * since the token was issued (QAD-02).
   *
   * @throws ActionNotAllowedException if they are not
   */
  static void requireActiveAuthority(AuthorizationPort authorization, AuthorityId authority) {
    if (!authorization.isActiveAuthority(authority)) {
      throw new ActionNotAllowedException(
          "User " + authority.value() + " is no longer an active authority");
    }
  }

  /**
   * The plan, if it belongs to the organization. A plan of another organization is answered as
   * not found, so an organization cannot learn which plans the others have.
   */
  static DistributionPlan load(
      DistributionPlanRepository repository,
      DistributionPlanId planId,
      OrganizationId organizationId) {
    return repository
        .findById(planId)
        .filter(plan -> plan.belongsTo(organizationId))
        .orElseThrow(() -> new DistributionPlanNotFoundException(planId));
  }
}
