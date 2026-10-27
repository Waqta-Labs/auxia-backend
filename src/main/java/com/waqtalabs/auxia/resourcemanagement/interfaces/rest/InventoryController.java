package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AdjustStockCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.CreateInventoryCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.RegisterResourceCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.RestockResourceCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetInventoryByIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetInventoryByOrganizationIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.InventoryQueryService;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.assemblers.InventoryResourceAssembler;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.AdjustStockRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.CreateInventoryRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.InventoryResource;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.RegisterResourceRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.RestockRequest;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.StockItemResource;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory of the organization of the signed-in user, which comes from the JWT. Any user of the
 * organization reads it; only an ORGANIZATION_ADMIN changes it. An inventory of another
 * organization is not found.
 */
@RestController
@RequestMapping("/api/v1/inventories")
@Tag(name = "Inventories", description = "Stock of humanitarian resources (US-08, US-09, TS-03)")
public class InventoryController {

  private final CreateInventoryCommandHandler createInventory;
  private final RegisterResourceCommandHandler registerResource;
  private final RestockResourceCommandHandler restockResource;
  private final AdjustStockCommandHandler adjustStock;
  private final InventoryQueryService inventoryQueries;

  public InventoryController(
      CreateInventoryCommandHandler createInventory,
      RegisterResourceCommandHandler registerResource,
      RestockResourceCommandHandler restockResource,
      AdjustStockCommandHandler adjustStock,
      InventoryQueryService inventoryQueries) {
    this.createInventory = createInventory;
    this.registerResource = registerResource;
    this.restockResource = restockResource;
    this.adjustStock = adjustStock;
    this.inventoryQueries = inventoryQueries;
  }

  @PostMapping
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(
      summary = "Create the inventory of the organization",
      description = "Each organization has a single inventory, which starts without resources.")
  @ApiResponse(responseCode = "201", description = "Inventory created")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "409", description = "The organization already has an inventory")
  public ResponseEntity<InventoryResource> create(
      @Valid @RequestBody CreateInventoryRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    Inventory inventory =
        createInventory.handle(InventoryResourceAssembler.toCommand(organizationOf(user), request));
    return ResponseEntity.created(URI.create("/api/v1/inventories/" + inventory.id().value()))
        .body(InventoryResourceAssembler.toResource(inventory, null));
  }

  @GetMapping
  @Operation(summary = "Get the inventory of the organization of the user")
  @ApiResponse(responseCode = "200", description = "Inventory of the organization")
  @ApiResponse(responseCode = "404", description = "The organization has no inventory")
  public InventoryResource findOwn(@AuthenticationPrincipal AuthenticatedUser user) {
    var query = new GetInventoryByOrganizationIdQuery(organizationOf(user));
    return InventoryResourceAssembler.toResource(inventoryQueries.handle(query), null);
  }

  @GetMapping("/{inventoryId}")
  @Operation(
      summary = "Get an inventory with the available stock of each resource",
      description =
          "available is what can still be distributed: the reserved quantity is held for"
              + " distribution plans (US-09, TS-03).")
  @ApiResponse(responseCode = "200", description = "Inventory found")
  @ApiResponse(responseCode = "404", description = "Not an inventory of the organization")
  public InventoryResource findById(
      @PathVariable UUID inventoryId,
      @Parameter(description = "Only the resources of this category")
          @RequestParam(required = false)
          ResourceCategory category,
      @AuthenticationPrincipal AuthenticatedUser user) {
    Inventory inventory =
        inventoryQueries.handle(
            new GetInventoryByIdQuery(new InventoryId(inventoryId), organizationOf(user)));
    return InventoryResourceAssembler.toResource(inventory, category);
  }

  @PostMapping("/{inventoryId}/resources")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(summary = "Register a resource", description = "It starts without stock.")
  @ApiResponse(responseCode = "201", description = "Resource registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "404", description = "Not an inventory of the organization")
  @ApiResponse(responseCode = "409", description = "A resource already has that name")
  public ResponseEntity<StockItemResource> registerResource(
      @PathVariable UUID inventoryId,
      @Valid @RequestBody RegisterResourceRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command = InventoryResourceAssembler.toCommand(inventoryId, organizationOf(user), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(InventoryResourceAssembler.toResource(registerResource.handle(command)));
  }

  @PostMapping("/{inventoryId}/resources/{resourceId}/restock")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(
      summary = "Register the entry of a resource",
      description = "Adds a quantity received, such as a donation or a purchase (US-08).")
  @ApiResponse(responseCode = "200", description = "Stock updated")
  @ApiResponse(responseCode = "400", description = "The quantity is missing, zero or negative")
  @ApiResponse(responseCode = "404", description = "The inventory or the resource does not exist")
  public StockItemResource restock(
      @PathVariable UUID inventoryId,
      @PathVariable UUID resourceId,
      @Valid @RequestBody RestockRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        InventoryResourceAssembler.toCommand(
            inventoryId, organizationOf(user), resourceId, request);
    return InventoryResourceAssembler.toResource(restockResource.handle(command));
  }

  @PatchMapping("/{inventoryId}/resources/{resourceId}/adjustment")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(
      summary = "Adjust the stock of a resource",
      description =
          "Adds or removes a quantity with a reason, for example after a count or a loss. It"
              + " cannot remove more than the available quantity (TS-03).")
  @ApiResponse(responseCode = "200", description = "Stock adjusted")
  @ApiResponse(responseCode = "400", description = "Invalid request, for example a change of 0")
  @ApiResponse(responseCode = "404", description = "The inventory or the resource does not exist")
  @ApiResponse(responseCode = "409", description = "It removes more than the available quantity")
  public StockItemResource adjust(
      @PathVariable UUID inventoryId,
      @PathVariable UUID resourceId,
      @Valid @RequestBody AdjustStockRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        InventoryResourceAssembler.toCommand(
            inventoryId, organizationOf(user), resourceId, request);
    return InventoryResourceAssembler.toResource(adjustStock.handle(command));
  }

  private static OrganizationId organizationOf(AuthenticatedUser user) {
    return new OrganizationId(user.organizationId());
  }
}
