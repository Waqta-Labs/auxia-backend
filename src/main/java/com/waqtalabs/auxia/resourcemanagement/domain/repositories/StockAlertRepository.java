package com.waqtalabs.auxia.resourcemanagement.domain.repositories;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import java.util.List;

public interface StockAlertRepository {

  StockAlert save(StockAlert alert);

  /**
   * Alerts of the inventory, the most recent first.
   *
   * @param status {@code null} for every alert
   */
  List<StockAlert> findByInventoryId(InventoryId inventoryId, StockAlertStatus status);
}
