package com.waqtalabs.auxia.traceability.application.queryservices;

import com.waqtalabs.auxia.traceability.application.ports.DeliveryReportRenderer;
import com.waqtalabs.auxia.traceability.application.ports.ResourceCatalogPort;
import com.waqtalabs.auxia.traceability.application.ports.ZoneNamePort;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveryReportQuery;
import com.waqtalabs.auxia.traceability.domain.exceptions.InvalidReportPeriodException;
import com.waqtalabs.auxia.traceability.domain.exceptions.NoDeliveriesToReportException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports the history of deliveries of the organization, for a zone or a period, to support the
 * aid delivered in an audit (US-19). Only the deliveries that happened are included: registered,
 * pending verification, or certified on Blockchain.
 */
@Service
@Transactional(readOnly = true)
public class DeliveryReportQueryService {

  private final DeliveryRepository deliveryRepository;
  private final ResourceCatalogPort resourceCatalog;
  private final ZoneNamePort zoneNames;
  private final DeliveryReportRenderer renderer;
  private final Clock clock;

  public DeliveryReportQueryService(
      DeliveryRepository deliveryRepository,
      ResourceCatalogPort resourceCatalog,
      ZoneNamePort zoneNames,
      DeliveryReportRenderer renderer,
      Clock clock) {
    this.deliveryRepository = deliveryRepository;
    this.resourceCatalog = resourceCatalog;
    this.zoneNames = zoneNames;
    this.renderer = renderer;
    this.clock = clock;
  }

  /**
   * @throws InvalidReportPeriodException if the period ends before it starts
   * @throws NoDeliveriesToReportException if no delivery matches (US-19, scenario 2)
   */
  public byte[] handle(GetDeliveryReportQuery query) {
    if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
      throw new InvalidReportPeriodException(query.from(), query.to());
    }
    List<Delivery> deliveries =
        deliveryRepository.findCompleted(
            query.organizationId(),
            query.zoneId(),
            query.from() == null ? null : query.from().atStartOfDay(),
            query.to() == null ? null : query.to().plusDays(1).atStartOfDay());
    if (deliveries.isEmpty()) {
      throw new NoDeliveriesToReportException();
    }
    Map<ResourceId, String> resources = resourceCatalog.resourceNames(query.organizationId());
    Map<ZoneId, String> zones =
        zoneNames.zoneNames(deliveries.stream().map(Delivery::zoneId).distinct().toList());
    List<DeliveryReport.Entry> entries =
        deliveries.stream()
            .map(
                delivery ->
                    new DeliveryReport.Entry(
                        delivery,
                        zones.get(delivery.zoneId()),
                        delivery.items().stream()
                            .map(
                                item ->
                                    new DeliveryReport.Item(
                                        resources.getOrDefault(
                                            item.resourceId(),
                                            item.resourceId().value().toString()),
                                        item.quantity()))
                            .toList()))
            .toList();
    var report =
        new DeliveryReport(
            query.zoneId() == null ? null : zones.get(query.zoneId()),
            query.from(),
            query.to(),
            LocalDateTime.now(clock),
            entries);
    return renderer.render(report, query.locale());
  }
}
