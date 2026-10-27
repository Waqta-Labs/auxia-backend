package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AdjustStockCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.CreateInventoryCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.RegisterResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.RestockResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.AdjustStockRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.CreateInventoryRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.InventoryResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.RegisterResourceRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.RestockRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.StockItemResource;
import java.util.Comparator;
import java.util.UUID;

public final class InventoryResourceAssembler {

  private InventoryResourceAssembler() {}

  public static CreateInventoryCommand toCommand(CreateInventoryRequest request) {
    return new CreateInventoryCommand(new OrganizationId(request.organizationId()), request.name());
  }

  /** A missing minimum stock is 0, as in the database. */
  public static RegisterResourceCommand toCommand(
      UUID inventoryId, RegisterResourceRequest request) {
    return new RegisterResourceCommand(
        new InventoryId(inventoryId),
        request.name(),
        request.category(),
        request.unit(),
        request.minimumStock() == null ? 0 : request.minimumStock());
  }

  public static RestockResourceCommand toCommand(
      UUID inventoryId, UUID resourceId, RestockRequest request) {
    return new RestockResourceCommand(
        new InventoryId(inventoryId), new ResourceId(resourceId), request.quantity());
  }

  public static AdjustStockCommand toCommand(
      UUID inventoryId, UUID resourceId, AdjustStockRequest request) {
    return new AdjustStockCommand(
        new InventoryId(inventoryId),
        new ResourceId(resourceId),
        request.quantityChange(),
        request.reason());
  }

  /**
   * The inventory with its resources ordered by name.
   *
   * @param category only the resources of this category, or every resource when {@code null}
   */
  public static InventoryResource toResource(Inventory inventory, ResourceCategory category) {
    return new InventoryResource(
        inventory.id().value(),
        inventory.organizationId().value(),
        inventory.name(),
        inventory.stockItems().stream()
            .filter(item -> category == null || item.category() == category)
            .sorted(Comparator.comparing(StockItem::name, String.CASE_INSENSITIVE_ORDER))
            .map(InventoryResourceAssembler::toResource)
            .toList());
  }

  public static StockItemResource toResource(StockItem item) {
    return new StockItemResource(
        item.resourceId().value(),
        item.name(),
        item.category(),
        item.unit(),
        item.stockLevel().onHand(),
        item.stockLevel().reserved(),
        item.available(),
        item.minimumStock(),
        item.isBelowMinimum());
  }
}
