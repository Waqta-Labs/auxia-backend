package com.waqtalabs.auxia.traceability.interfaces.acl;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What Traceability offers to the other bounded contexts. Resource Management uses it to give
 * each mission of a brigade the delivery that the brigade registers (A_07, A_08).
 */
@Service
public class TraceabilityContextFacade {

  private final DeliveryRepository deliveryRepository;

  public TraceabilityContextFacade(DeliveryRepository deliveryRepository) {
    this.deliveryRepository = deliveryRepository;
  }

  /** The delivery of a plan, opened when a brigade took it; empty before that. */
  @Transactional(readOnly = true)
  public Optional<UUID> fetchDeliveryOfPlan(UUID distributionPlanId) {
    return deliveryRepository
        .findByDistributionPlanId(new DistributionPlanId(distributionPlanId))
        .map(Delivery::id)
        .map(id -> id.value());
  }
}
