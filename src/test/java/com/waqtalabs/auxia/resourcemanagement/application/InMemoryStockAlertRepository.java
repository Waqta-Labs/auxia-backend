package com.waqtalabs.auxia.resourcemanagement.application;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.StockAlertRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test double of the repository. */
public class InMemoryStockAlertRepository implements StockAlertRepository {

  private final Map<StockAlertId, StockAlert> alerts = new LinkedHashMap<>();

  @Override
  public StockAlert save(StockAlert alert) {
    alerts.put(alert.id(), alert);
    return alert;
  }

  @Override
  public List<StockAlert> findByInventoryId(InventoryId inventoryId, StockAlertStatus status) {
    return alerts.values().stream()
        .filter(alert -> alert.inventoryId().equals(inventoryId))
        .filter(alert -> status == null || alert.status() == status)
        .toList()
        .reversed();
  }
}
