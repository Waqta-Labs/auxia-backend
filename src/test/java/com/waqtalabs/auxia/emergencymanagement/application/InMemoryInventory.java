package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.application.ports.InventoryAvailabilityPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.ResourceReservationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test double of the inventory of Resource Management for one organization. A reservation lowers
 * the available quantity, and asking for more than it raises IllegalStateException.
 */
public class InMemoryInventory implements InventoryAvailabilityPort, ResourceReservationPort {

  private final OrganizationId organizationId;
  private final Map<ResourceId, AvailableResource> resources = new LinkedHashMap<>();
  private final Map<DistributionPlanId, List<ResourceQuantity>> reservations = new HashMap<>();

  public InMemoryInventory(OrganizationId organizationId) {
    this.organizationId = organizationId;
  }

  public ResourceId add(String name, String category, int available) {
    var id = new ResourceId(UUID.randomUUID());
    resources.put(id, new AvailableResource(id, name, category, "UNIT", available));
    return id;
  }

  public int available(ResourceId resourceId) {
    return resources.get(resourceId).available();
  }

  /** Quantities reserved for the plan; empty if it holds nothing. */
  public List<ResourceQuantity> reservationOf(DistributionPlanId planId) {
    return reservations.getOrDefault(planId, List.of());
  }

  @Override
  public List<AvailableResource> availableStock(OrganizationId organizationId) {
    return this.organizationId.equals(organizationId)
        ? new ArrayList<>(resources.values())
        : List.of();
  }

  @Override
  public void reserve(
      OrganizationId organizationId, DistributionPlanId planId, List<ResourceQuantity> quantities) {
    if (reservations.containsKey(planId)) {
      throw new IllegalStateException("plan " + planId.value() + " already reserved");
    }
    hold(quantities);
    reservations.put(planId, List.copyOf(quantities));
  }

  @Override
  public void change(DistributionPlanId planId, List<ResourceQuantity> quantities) {
    List<ResourceQuantity> held = reservations.remove(planId);
    giveBack(held);
    try {
      hold(quantities);
    } catch (IllegalStateException ex) {
      hold(held);
      reservations.put(planId, held);
      throw ex;
    }
    reservations.put(planId, List.copyOf(quantities));
  }

  @Override
  public void release(DistributionPlanId planId) {
    giveBack(reservations.remove(planId));
  }

  private void hold(List<ResourceQuantity> quantities) {
    for (ResourceQuantity quantity : quantities) {
      AvailableResource resource = resources.get(quantity.resourceId());
      if (resource == null || resource.available() < quantity.quantity()) {
        throw new IllegalStateException("not enough " + quantity.resourceId().value());
      }
    }
    quantities.forEach(quantity -> update(quantity.resourceId(), -quantity.quantity()));
  }

  private void giveBack(List<ResourceQuantity> quantities) {
    if (quantities == null) {
      throw new IllegalStateException("no reservation");
    }
    quantities.forEach(quantity -> update(quantity.resourceId(), quantity.quantity()));
  }

  private void update(ResourceId id, int change) {
    AvailableResource resource = resources.get(id);
    resources.put(
        id,
        new AvailableResource(
            id,
            resource.name(),
            resource.category(),
            resource.unit(),
            resource.available() + change));
  }
}
