package com.waqtalabs.auxia.traceability.application.queryservices;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * History of deliveries of a zone or period, as it is exported for an audit (US-19).
 *
 * @param zoneName {@code null} when the report covers every zone
 * @param from {@code null} when the period has no start
 * @param to {@code null} when the period has no end
 * @param deliveries from the oldest to the most recent
 */
public record DeliveryReport(
    String zoneName,
    LocalDate from,
    LocalDate to,
    LocalDateTime generatedAt,
    List<Entry> deliveries) {

  /**
   * A delivery with the names of its zone and resources.
   *
   * @param zoneName {@code null} if the zone no longer exists
   */
  public record Entry(Delivery delivery, String zoneName, List<Item> items) {}

  /** A delivered resource; its name is its identifier if it is not in the inventory. */
  public record Item(String resourceName, int quantity) {}
}
