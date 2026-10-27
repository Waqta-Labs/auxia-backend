package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReleaseReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReserveResourcesCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReleaseReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReserveResourcesCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What Resource Management offers to the other bounded contexts, which run in the same process
 * (Shared Kernel). Emergency Management uses it to check the available stock of an organization
 * and to reserve or release the resources of a distribution plan.
 *
 * <p>It receives and returns UUIDs and simple records, so no other context depends on the domain
 * of Resource Management. Errors are the domain exceptions of this context: 404 when the
 * organization has no inventory or the resource does not exist, 409 when the stock is not
 * enough.
 */
@Service
public class ResourceManagementContextFacade {

  private final InventoryRepository inventoryRepository;
  private final ReserveResourcesCommandHandler reserveResources;
  private final ReleaseReservationCommandHandler releaseReservation;

  public ResourceManagementContextFacade(
      InventoryRepository inventoryRepository,
      ReserveResourcesCommandHandler reserveResources,
      ReleaseReservationCommandHandler releaseReservation) {
    this.inventoryRepository = inventoryRepository;
    this.reserveResources = reserveResources;
    this.releaseReservation = releaseReservation;
  }

  /** Resources of the inventory of an organization; empty when it has no inventory. */
  @Transactional(readOnly = true)
  public List<AvailableStockResource> fetchAvailableStock(UUID organizationId) {
    return inventoryRepository
        .findByOrganizationId(new OrganizationId(organizationId))
        .map(
            inventory ->
                inventory.stockItems().stream()
                    .map(
                        item ->
                            new AvailableStockResource(
                                item.resourceId().value(),
                                item.name(),
                                item.category().name(),
                                item.unit().name(),
                                item.available()))
                    .toList())
        .orElse(List.of());
  }

  /** Reserves every line for the plan, or none if one line cannot be reserved. */
  public void reserveResources(
      UUID organizationId, UUID distributionPlanId, List<ReservationLineResource> lines) {
    reserveResources.handle(
        new ReserveResourcesCommand(
            new OrganizationId(organizationId),
            new DistributionPlanId(distributionPlanId),
            lines.stream()
                .map(
                    line ->
                        new ReservationLine(new ResourceId(line.resourceId()), line.quantity()))
                .toList()));
  }

  /** Makes the stock held for the plan available again. */
  public void releaseReservation(UUID distributionPlanId) {
    releaseReservation.handle(
        new ReleaseReservationCommand(new DistributionPlanId(distributionPlanId)));
  }
}
