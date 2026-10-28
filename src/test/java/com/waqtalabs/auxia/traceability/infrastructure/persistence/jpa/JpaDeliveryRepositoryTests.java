package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.traceability.application.Sha256;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.DeliveryItem;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.AnchorStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JpaDeliveryRepositoryTests {

  private static final LocalDateTime DELIVERED_AT = LocalDateTime.of(2026, 3, 15, 10, 30);
  /** With microseconds, the precision of the database. */
  private static final LocalDateTime RECEIVED_AT =
      LocalDateTime.of(2026, 3, 15, 18, 0, 0, 123_000);
  private static final String TRANSACTION = "0x" + "c".repeat(64);

  @Autowired private DeliveryRepository deliveries;
  @Autowired private EntityManager entityManager;

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final ZoneId zone = new ZoneId(UUID.randomUUID());
  private final ResourceId water = new ResourceId(UUID.randomUUID());

  @Test
  void openedDeliveryIsStoredWithoutRegistration() {
    Delivery delivery = deliveries.save(initiated());
    clear();

    Delivery found = deliveries.findById(delivery.id()).orElseThrow();
    assertThat(found.status()).isEqualTo(DeliveryStatus.INITIATED);
    assertThat(found.organizationId()).isEqualTo(organization);
    assertThat(found.registration()).isEmpty();
    assertThat(found.integrity()).isEmpty();
    assertThat(deliveries.findByDistributionPlanId(delivery.distributionPlanId()))
        .map(Delivery::id)
        .contains(delivery.id());
  }

  @Test
  void registeredDeliveryKeepsItsItemsEvidenceAndHash() {
    Delivery delivery = registered();
    clear();

    Delivery found = deliveries.findById(delivery.id()).orElseThrow();
    assertThat(found.status()).isEqualTo(DeliveryStatus.REGISTERED);
    assertThat(found.registration()).isEqualTo(delivery.registration());
    assertThat(found.items())
        .extracting(DeliveryItem::resourceId, DeliveryItem::quantity)
        .containsExactly(tuple(water, 300));
    Evidence evidence = found.evidences().getFirst();
    assertThat(evidence.type()).isEqualTo(EvidenceType.PHOTO);
    assertThat(evidence.contentHash()).isEqualTo(new HashValue("a".repeat(64)));
    assertThat(evidence.location()).contains(new GeoPoint(-11.9436, -76.7078));
    assertThat(found.integrity()).isEqualTo(delivery.integrity());
    assertThat(found.canonicalRecord()).isEqualTo(delivery.canonicalRecord());
  }

  @Test
  void transactionIsKeptFromItsSendingUntilItsConfirmation() {
    Delivery delivery = registered();
    delivery.markSubmitted(TRANSACTION);
    deliveries.save(delivery);
    clear();

    Delivery submitted = deliveries.findById(delivery.id()).orElseThrow();
    assertThat(submitted.integrity().orElseThrow().transaction()).contains(TRANSACTION);
    assertThat(deliveries.findPendingAnchoring()).extracting(Delivery::id).contains(delivery.id());

    submitted.markAnchored(RECEIVED_AT.plusMinutes(1));
    deliveries.save(submitted);
    clear();

    Delivery certified = deliveries.findById(delivery.id()).orElseThrow();
    assertThat(certified.status()).isEqualTo(DeliveryStatus.CERTIFIED);
    assertThat(certified.integrity().orElseThrow().status()).isEqualTo(AnchorStatus.ANCHORED);
    assertThat(certified.integrity().orElseThrow().attempts()).isEqualTo(1);
    assertThat(deliveries.findPendingAnchoring())
        .extracting(Delivery::id)
        .doesNotContain(delivery.id());
  }

  @Test
  void deliveriesOfAZoneAreThoseOfTheOrganization() {
    Delivery first = deliveries.save(initiated());
    Delivery other =
        deliveries.save(
            Delivery.initiate(
                new DistributionPlanId(UUID.randomUUID()),
                zone,
                new BrigadeId(UUID.randomUUID()),
                new OrganizationId(UUID.randomUUID())));
    clear();

    assertThat(deliveries.findByZoneId(zone, organization))
        .extracting(Delivery::id)
        .containsExactly(first.id())
        .doesNotContain(other.id());
  }

  @Test
  void planHasOnlyOneDelivery() {
    Delivery delivery = deliveries.save(initiated());
    deliveries.save(
        Delivery.initiate(
            delivery.distributionPlanId(),
            zone,
            new BrigadeId(UUID.randomUUID()),
            organization));

    assertThatThrownBy(this::clear).hasMessageContaining("uq_delivery_plan");
  }

  private Delivery initiated() {
    return Delivery.initiate(
        new DistributionPlanId(UUID.randomUUID()),
        zone,
        new BrigadeId(UUID.randomUUID()),
        organization);
  }

  private Delivery registered() {
    Delivery delivery = initiated();
    delivery.register(
        List.of(DeliveryItem.of(water, 300)),
        List.of(
            Evidence.attach(
                EvidenceType.PHOTO,
                "http://storage/evidences/photo",
                new HashValue("a".repeat(64)),
                DELIVERED_AT,
                new GeoPoint(-11.9436, -76.7078))),
        new UserId(UUID.randomUUID()),
        DELIVERED_AT,
        RECEIVED_AT,
        new Sha256());
    return deliveries.save(delivery);
  }

  private void clear() {
    entityManager.flush();
    entityManager.clear();
  }
}
