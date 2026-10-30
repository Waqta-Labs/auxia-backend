package com.waqtalabs.auxia.resourcemanagement.application.ports;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;

/** Tells the people of the organization that a resource is running out (US-10). */
@FunctionalInterface
public interface StockAlertNotifier {

  void notify(StockAlert alert);
}
