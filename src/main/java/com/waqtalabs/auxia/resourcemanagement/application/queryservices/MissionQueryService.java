package com.waqtalabs.auxia.resourcemanagement.application.queryservices;

import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionDeliveries;
import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionZones;
import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionZones.MissionZone;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetMyMissionsQuery;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.BrigadeAssignment;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Missions of a brigadist (A_07): the assignments of the brigade their account is a member of,
 * the one in progress first and then the completed ones, from the most recent. Each one carries
 * the zone, what to deliver and the delivery to register.
 */
@Service
@Transactional(readOnly = true)
public class MissionQueryService {

  private final BrigadeRepository brigadeRepository;
  private final InventoryRepository inventoryRepository;
  private final MissionZones missionZones;
  private final MissionDeliveries missionDeliveries;

  public MissionQueryService(
      BrigadeRepository brigadeRepository,
      InventoryRepository inventoryRepository,
      MissionZones missionZones,
      MissionDeliveries missionDeliveries) {
    this.brigadeRepository = brigadeRepository;
    this.inventoryRepository = inventoryRepository;
    this.missionZones = missionZones;
    this.missionDeliveries = missionDeliveries;
  }

  /**
   * The brigade and its missions.
   *
   * @param brigade empty when the account is not a member of any brigade
   */
  public record MyMissions(Optional<Brigade> brigade, List<Mission> missions) {}

  /**
   * A mission card (A_07).
   *
   * @param deliveryId the delivery to register; empty while Traceability has not opened it
   * @param category {@code MEDICAL} when the plan carries medicine ("Médico"), else {@code
   *     SUPPLIES} ("Víveres")
   */
  public record Mission(
      Optional<UUID> deliveryId,
      UUID planId,
      AssignmentStatus status,
      String category,
      LocalDateTime assignedAt,
      Optional<LocalDateTime> completedAt,
      MissionZone zone,
      List<Item> items) {}

  /** A resource to deliver, with its name in the inventory. */
  public record Item(ResourceId resourceId, String name, UnitOfMeasure unit, int quantity) {}

  public MyMissions handle(GetMyMissionsQuery query) {
    Optional<Brigade> brigade = brigadeRepository.findByMemberAccount(query.userId());
    if (brigade.isEmpty()) {
      return new MyMissions(Optional.empty(), List.of());
    }
    Map<ResourceId, StockItem> stock =
        inventoryRepository
            .findByOrganizationId(brigade.get().organizationId())
            .map(Inventory::stockItems)
            .orElse(List.of())
            .stream()
            .collect(Collectors.toMap(StockItem::resourceId, Function.identity()));
    List<Mission> missions =
        brigade.get().assignments().stream()
            .sorted(
                Comparator.comparing((BrigadeAssignment assignment) -> !assignment.isInProgress())
                    .thenComparing(BrigadeAssignment::assignedAt, Comparator.reverseOrder()))
            .flatMap(assignment -> mission(brigade.get(), assignment, stock).stream())
            .toList();
    return new MyMissions(brigade, missions);
  }

  private Optional<Mission> mission(
      Brigade brigade, BrigadeAssignment assignment, Map<ResourceId, StockItem> stock) {
    return missionZones
        .zoneOf(assignment.distributionPlanId(), brigade.baseLocation())
        .map(
            zone -> {
              List<Item> items =
                  zone.items().stream()
                      .map(
                          item -> {
                            StockItem stocked = stock.get(item.resourceId());
                            return new Item(
                                item.resourceId(),
                                stocked == null ? "" : stocked.name(),
                                stocked == null ? null : stocked.unit(),
                                item.quantity());
                          })
                      .toList();
              boolean medical =
                  zone.items().stream()
                      .map(item -> stock.get(item.resourceId()))
                      .anyMatch(
                          stocked ->
                              stocked != null && stocked.category() == ResourceCategory.MEDICINE);
              return new Mission(
                  missionDeliveries.deliveryOf(assignment.distributionPlanId()),
                  assignment.distributionPlanId().value(),
                  assignment.status(),
                  medical ? "MEDICAL" : "SUPPLIES",
                  assignment.assignedAt(),
                  assignment.completedAt(),
                  zone,
                  items);
            });
  }
}
