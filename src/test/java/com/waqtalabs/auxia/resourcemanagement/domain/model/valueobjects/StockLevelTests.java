package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StockLevelTests {

  @Test
  void availableIsWhatIsNotReserved() {
    assertThat(new StockLevel(100, 60).available()).isEqualTo(40);
    assertThat(StockLevel.empty().available()).isZero();
  }

  @Test
  void reservedCannotExceedOnHand() {
    assertThatThrownBy(() -> new StockLevel(10, 11)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void quantitiesCannotBeNegative() {
    assertThatThrownBy(() -> new StockLevel(-1, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reservationLineNeedsAPositiveQuantity() {
    assertThatThrownBy(() -> new ReservationLine(ResourceId.generate(), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
