package com.waqtalabs.auxia.resourcemanagement.application.queryservices;

import com.waqtalabs.auxia.resourcemanagement.application.queries.GetStockAlertsQuery;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.StockAlertRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Low stock alerts of an inventory. An inventory of another organization is not found. */
@Service
@Transactional(readOnly = true)
public class StockAlertQueryService {

  private final InventoryRepository inventoryRepository;
  private final StockAlertRepository stockAlertRepository;

  public StockAlertQueryService(
      InventoryRepository inventoryRepository, StockAlertRepository stockAlertRepository) {
    this.inventoryRepository = inventoryRepository;
    this.stockAlertRepository = stockAlertRepository;
  }

  public List<StockAlert> handle(GetStockAlertsQuery query) {
    inventoryRepository
        .findById(query.inventoryId())
        .filter(inventory -> inventory.belongsTo(query.organizationId()))
        .orElseThrow(() -> new InventoryNotFoundException(query.inventoryId()));
    return stockAlertRepository.findByInventoryId(query.inventoryId(), query.status());
  }
}
