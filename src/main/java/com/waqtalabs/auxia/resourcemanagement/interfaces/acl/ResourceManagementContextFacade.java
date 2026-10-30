package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AssignBrigadeCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ChangeReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReleaseReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReserveResourcesCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.AssignBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ChangeReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReleaseReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReserveResourcesCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What Resource Management offers to the other bounded contexts, which run in the same process
 * (Shared Kernel). Emergency Management uses it to check the available stock of an organization,
 * to reserve or release the resources of a distribution plan and to assign it a brigade.
 *
 * <p>It receives and returns UUIDs and simple records, so no other context depends on the domain
 * of Resource Management. Errors are the domain exceptions of this context: 404 when the
 * organization has no inventory or the resource does not exist, 409 when the stock is not
 * enough or the plan already has a brigade.
 */
@Service
public class ResourceManagementContextFacade {

  private final InventoryRepository inventoryRepository;
  private final ReserveResourcesCommandHandler reserveResources;
  private final ChangeReservationCommandHandler changeReservation;
  private final ReleaseReservationCommandHandler releaseReservation;
  private final AssignBrigadeCommandHandler assignBrigade;

  public ResourceManagementContextFacade(
      InventoryRepository inventoryRepository,
      ReserveResourcesCommandHandler reserveResources,
      ChangeReservationCommandHandler changeReservation,
      ReleaseReservationCommandHandler releaseReservation,
      AssignBrigadeCommandHandler assignBrigade) {
    this.inventoryRepository = inventoryRepository;
    this.reserveResources = reserveResources;
    this.changeReservation = changeReservation;
    this.releaseReservation = releaseReservation;
    this.assignBrigade = assignBrigade;
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
            toReservationLines(lines)));
  }

  /**
   * Replaces the lines held for the plan. What the plan already holds counts as available, and
   * nothing changes if one line cannot be reserved.
   */
  public void changeReservation(UUID distributionPlanId, List<ReservationLineResource> lines) {
    changeReservation.handle(
        new ChangeReservationCommand(
            new DistributionPlanId(distributionPlanId), toReservationLines(lines)));
  }

  /** Makes the stock held for the plan available again. */
  public void releaseReservation(UUID distributionPlanId) {
    releaseReservation.handle(
        new ReleaseReservationCommand(new DistributionPlanId(distributionPlanId)));
  }

  /**
   * Assigns a brigade of the organization to an approved plan. Traceability opens the delivery
   * when the transaction is committed.
   *
   * @param zoneLocation where the zone is, or {@code null} when it has no coordinates
   * @return the assigned brigade, or empty when no brigade of the organization can take the plan
   *     now
   */
  public Optional<UUID> assignBrigade(
      UUID organizationId,
      UUID distributionPlanId,
      UUID zoneId,
      ZoneLocationResource zoneLocation) {
    return assignBrigade
        .handle(
            new AssignBrigadeCommand(
                new OrganizationId(organizationId),
                new DistributionPlanId(distributionPlanId),
                new ZoneId(zoneId),
                Optional.ofNullable(zoneLocation)
                    .map(location -> new Location(location.latitude(), location.longitude()))))
        .map(brigade -> brigade.id().value());
  }

  private static List<ReservationLine> toReservationLines(List<ReservationLineResource> lines) {
    return lines.stream()
        .map(line -> new ReservationLine(new ResourceId(line.resourceId()), line.quantity()))
        .toList();
  }
}
