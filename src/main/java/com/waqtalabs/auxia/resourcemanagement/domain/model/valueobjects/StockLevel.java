package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

/**
 * Physical and reserved quantity of a resource. What the authority can still distribute is
 * {@link #available()} (US-09).
 *
 * @param onHand quantity in the warehouse
 * @param reserved part of {@code onHand} held for distribution plans
 */
public record StockLevel(int onHand, int reserved) {

  public StockLevel {
    if (onHand < 0 || reserved < 0) {
      throw new IllegalArgumentException("quantities must not be negative");
    }
    if (reserved > onHand) {
      throw new IllegalArgumentException("reserved cannot be greater than onHand");
    }
  }

  public static StockLevel empty() {
    return new StockLevel(0, 0);
  }

  public int available() {
    return onHand - reserved;
  }
}
