package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import java.util.List;

/**
 * Holds in Resource Management the stock of a distribution plan while it is decided, so two plans
 * never count on the same units. Every operation changes all the quantities or none.
 */
public interface ResourceReservationPort {

  void reserve(
      OrganizationId organizationId, DistributionPlanId planId, List<ResourceQuantity> quantities);

  /** Replaces the quantities held for the plan after a modification. */
  void change(DistributionPlanId planId, List<ResourceQuantity> quantities);

  /** Makes the stock held for the plan available again, for example when it is rejected. */
  void release(DistributionPlanId planId);
}
