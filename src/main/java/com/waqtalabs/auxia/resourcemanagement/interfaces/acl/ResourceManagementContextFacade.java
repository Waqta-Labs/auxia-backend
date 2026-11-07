package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AddStaffMemberCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AssignBrigadeCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ChangeReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReleaseReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReserveResourcesCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.AddStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.AssignBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ChangeReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReleaseReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReserveResourcesCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What Resource Management offers to the other bounded contexts, which run in the same process
 * (Shared Kernel). Emergency Management uses it to check the available stock of an organization,
 * to reserve or release the resources of a distribution plan, to assign it a brigade and to know
 * whether a brigade took it.
 *
 * <p>It receives and returns UUIDs and simple records, so no other context depends on the domain
 * of Resource Management. Errors are the domain exceptions of this context: 404 when the
 * organization has no inventory or the resource does not exist, 409 when the stock is not
 * enough or the plan already has a brigade.
 */
@Service
public class ResourceManagementContextFacade {

  private final InventoryRepository inventoryRepository;
  private final BrigadeRepository brigadeRepository;
  private final ReserveResourcesCommandHandler reserveResources;
  private final ChangeReservationCommandHandler changeReservation;
  private final ReleaseReservationCommandHandler releaseReservation;
  private final AssignBrigadeCommandHandler assignBrigade;
  private final AddStaffMemberCommandHandler addStaffMember;

  public ResourceManagementContextFacade(
      InventoryRepository inventoryRepository,
      BrigadeRepository brigadeRepository,
      ReserveResourcesCommandHandler reserveResources,
      ChangeReservationCommandHandler changeReservation,
      ReleaseReservationCommandHandler releaseReservation,
      AssignBrigadeCommandHandler assignBrigade,
      AddStaffMemberCommandHandler addStaffMember) {
    this.inventoryRepository = inventoryRepository;
    this.brigadeRepository = brigadeRepository;
    this.reserveResources = reserveResources;
    this.changeReservation = changeReservation;
    this.releaseReservation = releaseReservation;
    this.assignBrigade = assignBrigade;
    this.addStaffMember = addStaffMember;
  }

  /**
   * The brigade with a code, which a brigadist writes to register (A_02), ignoring case; empty
   * when no brigade has it.
   */
  @Transactional(readOnly = true)
  public Optional<BrigadeReferenceResource> findBrigadeByCode(String code) {
    return brigadeRepository
        .findByCode(Brigade.normalizeCode(code))
        .map(
            brigade ->
                new BrigadeReferenceResource(
                    brigade.id().value(), brigade.organizationId().value(), brigade.name()));
  }

  /**
   * Adds an approved brigadist to the brigade as a {@code VOLUNTEER} linked to their
   * FIELD_BRIGADE account, so the app shows them its missions. Runs in the transaction of the
   * approval.
   */
  @Transactional
  public void addBrigadist(
      UUID brigadeId, UUID organizationId, String fullName, String phone, UUID userId) {
    addStaffMember.handle(
        new AddStaffMemberCommand(
            new BrigadeId(brigadeId),
            new OrganizationId(organizationId),
            fullName,
            StaffRole.VOLUNTEER,
            phone,
            new UserId(userId)));
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

  /**
   * Base of the brigade assigned to the plan, whether it is still delivering it or already done;
   * empty when no brigade took it.
   */
  @Transactional(readOnly = true)
  public Optional<BrigadeBaseResource> assignedBrigadeBase(UUID distributionPlanId) {
    return brigadeRepository
        .findByAssignedPlan(new DistributionPlanId(distributionPlanId))
        .map(Brigade::baseLocation)
        .map(base -> new BrigadeBaseResource(base.latitude(), base.longitude()));
  }

  private static List<ReservationLine> toReservationLines(List<ReservationLineResource> lines) {
    return lines.stream()
        .map(line -> new ReservationLine(new ResourceId(line.resourceId()), line.quantity()))
        .toList();
  }
}
