package com.waqtalabs.auxia.traceability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.traceability.application.queries.GetDeliveryReportQuery;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryReport;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryReportQueryService;
import com.waqtalabs.auxia.traceability.domain.exceptions.InvalidReportPeriodException;
import com.waqtalabs.auxia.traceability.domain.exceptions.NoDeliveriesToReportException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.DeliveryItem;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** History of deliveries of a zone or period, exported for an audit (US-19). */
class DeliveryReportUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-20T12:00:00Z"), ZoneOffset.UTC);
  private static final OrganizationId COER = new OrganizationId(UUID.randomUUID());
  private static final ZoneId ALTO_PERU = new ZoneId(UUID.randomUUID());
  private static final ZoneId PEDREGAL = new ZoneId(UUID.randomUUID());
  private static final ResourceId WATER = new ResourceId(UUID.randomUUID());
  private static final ResourceId UNKNOWN = new ResourceId(UUID.randomUUID());

  private final InMemoryDeliveryRepository deliveries = new InMemoryDeliveryRepository();
  private final List<DeliveryReport> rendered = new ArrayList<>();
  private final DeliveryReportQueryService reports =
      new DeliveryReportQueryService(
          deliveries,
          organization -> Map.of(WATER, "Agua 2.5 L"),
          zones -> Map.of(ALTO_PERU, "Sector Alto Perú", PEDREGAL, "Quebrada Pedregal"),
          (report, locale) -> {
            rendered.add(report);
            return new byte[] {1};
          },
          CLOCK);

  @Test
  void theReportOfAZoneListsItsDeliveriesFromTheOldestWithTheirNames() {
    Delivery second = registered(COER, ALTO_PERU, day(17), DeliveryItem.of(WATER, 80));
    Delivery first = registered(COER, ALTO_PERU, day(15), DeliveryItem.of(WATER, 120));
    registered(COER, PEDREGAL, day(16), DeliveryItem.of(WATER, 50));
    var otherOrganization = new OrganizationId(UUID.randomUUID());
    registered(otherOrganization, ALTO_PERU, day(16), DeliveryItem.of(WATER, 9));
    deliveries.save(initiated(COER, ALTO_PERU));

    byte[] pdf = reports.handle(query(ALTO_PERU, null, null));

    assertThat(pdf).containsExactly(1);
    DeliveryReport report = rendered.getFirst();
    assertThat(report.zoneName()).isEqualTo("Sector Alto Perú");
    assertThat(report.generatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 12, 0));
    assertThat(report.deliveries())
        .extracting(entry -> entry.delivery().id())
        .containsExactly(first.id(), second.id());
    assertThat(report.deliveries().getFirst().zoneName()).isEqualTo("Sector Alto Perú");
    assertThat(report.deliveries().getFirst().items())
        .containsExactly(new DeliveryReport.Item("Agua 2.5 L", 120));
  }

  @Test
  void bothDaysOfThePeriodAreIncluded() {
    registered(COER, ALTO_PERU, day(14), DeliveryItem.of(WATER, 10));
    Delivery start = registered(COER, PEDREGAL, LocalDateTime.of(2026, 3, 15, 0, 0), item());
    Delivery end = registered(COER, ALTO_PERU, LocalDateTime.of(2026, 3, 16, 23, 59), item());
    registered(COER, ALTO_PERU, LocalDateTime.of(2026, 3, 17, 0, 0), item());

    reports.handle(query(null, LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 16)));

    DeliveryReport report = rendered.getFirst();
    assertThat(report.zoneName()).isNull();
    assertThat(report.deliveries())
        .extracting(entry -> entry.delivery().id())
        .containsExactly(start.id(), end.id());
  }

  @Test
  void aResourceNotInTheInventoryIsNamedByItsIdentifier() {
    registered(COER, ALTO_PERU, day(15), DeliveryItem.of(UNKNOWN, 5));

    reports.handle(query(ALTO_PERU, null, null));

    assertThat(rendered.getFirst().deliveries().getFirst().items())
        .containsExactly(new DeliveryReport.Item(UNKNOWN.value().toString(), 5));
  }

  @Test
  void withoutDeliveriesInTheZoneOrPeriodThereIsNothingToExport() {
    registered(COER, ALTO_PERU, day(15), item());
    deliveries.save(initiated(COER, PEDREGAL));

    assertThatThrownBy(() -> reports.handle(query(PEDREGAL, null, null)))
        .isInstanceOf(NoDeliveriesToReportException.class);
    assertThatThrownBy(
            () -> reports.handle(query(null, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 31))))
        .isInstanceOf(NoDeliveriesToReportException.class);
    assertThat(rendered).isEmpty();
  }

  @Test
  void aPeriodEndsAfterItStarts() {
    assertThatThrownBy(
            () -> reports.handle(query(null, LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 15))))
        .isInstanceOf(InvalidReportPeriodException.class);
  }

  private Delivery registered(
      OrganizationId organization, ZoneId zone, LocalDateTime deliveredAt, DeliveryItem item) {
    Delivery delivery = initiated(organization, zone);
    delivery.register(
        List.of(item),
        List.of(
            Evidence.attach(
                EvidenceType.PHOTO,
                "https://storage/evidences/a",
                new HashValue("a".repeat(64)),
                deliveredAt,
                null)),
        new UserId(UUID.randomUUID()),
        deliveredAt,
        LocalDateTime.of(2026, 3, 20, 8, 0),
        new Sha256());
    return deliveries.save(delivery);
  }

  private static Delivery initiated(OrganizationId organization, ZoneId zone) {
    return Delivery.initiate(
        new DistributionPlanId(UUID.randomUUID()),
        zone,
        new BrigadeId(UUID.randomUUID()),
        organization);
  }

  private static DeliveryItem item() {
    return DeliveryItem.of(WATER, 10);
  }

  private static LocalDateTime day(int day) {
    return LocalDateTime.of(2026, 3, day, 10, 30);
  }

  private static GetDeliveryReportQuery query(ZoneId zone, LocalDate from, LocalDate to) {
    return new GetDeliveryReportQuery(COER, zone, from, to, Locale.ENGLISH);
  }
}
