package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.resourcemanagement;

import com.waqtalabs.auxia.emergencymanagement.application.ports.InventoryAvailabilityPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.ResourceReservationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ReservationLineResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ResourceManagementContextFacade;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter to the Open Host Service of Resource Management. It is the only class of Emergency
 * Management that knows its facade, and it translates its records into types of this context.
 */
@Component
class ResourceManagementInventoryAdapter
    implements InventoryAvailabilityPort, ResourceReservationPort {

  private final ResourceManagementContextFacade resourceManagement;

  ResourceManagementInventoryAdapter(ResourceManagementContextFacade resourceManagement) {
    this.resourceManagement = resourceManagement;
  }

  @Override
  public List<AvailableResource> availableStock(OrganizationId organizationId) {
    return resourceManagement.fetchAvailableStock(organizationId.value()).stream()
        .map(
            stock ->
                new AvailableResource(
                    new ResourceId(stock.resourceId()),
                    stock.name(),
                    stock.category(),
                    stock.unit(),
                    stock.available()))
        .toList();
  }

  @Override
  public void reserve(
      OrganizationId organizationId, DistributionPlanId planId, List<ResourceQuantity> quantities) {
    resourceManagement.reserveResources(
        organizationId.value(), planId.value(), toLines(quantities));
  }

  @Override
  public void change(DistributionPlanId planId, List<ResourceQuantity> quantities) {
    resourceManagement.changeReservation(planId.value(), toLines(quantities));
  }

  @Override
  public void release(DistributionPlanId planId) {
    resourceManagement.releaseReservation(planId.value());
  }

  private static List<ReservationLineResource> toLines(List<ResourceQuantity> quantities) {
    return quantities.stream()
        .map(
            quantity ->
                new ReservationLineResource(quantity.resourceId().value(), quantity.quantity()))
        .toList();
  }
}
