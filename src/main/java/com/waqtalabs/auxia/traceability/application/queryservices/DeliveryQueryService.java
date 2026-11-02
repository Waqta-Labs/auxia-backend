package com.waqtalabs.auxia.traceability.application.queryservices;

import com.waqtalabs.auxia.traceability.application.queries.GetDeliveriesByZoneIdQuery;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveryByIdQuery;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryNotFoundException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deliveries of the organization of the user. One of another organization is not found. */
@Service
@Transactional(readOnly = true)
public class DeliveryQueryService {

  private final DeliveryRepository deliveryRepository;

  public DeliveryQueryService(DeliveryRepository deliveryRepository) {
    this.deliveryRepository = deliveryRepository;
  }

  public Delivery handle(GetDeliveryByIdQuery query) {
    return deliveryRepository
        .findById(query.deliveryId())
        .filter(delivery -> delivery.belongsTo(query.organizationId()))
        .orElseThrow(() -> new DeliveryNotFoundException(query.deliveryId()));
  }

  /** The most recent first. */
  public List<Delivery> handle(GetDeliveriesByZoneIdQuery query) {
    return deliveryRepository.findByZoneId(query.zoneId(), query.organizationId());
  }
}
