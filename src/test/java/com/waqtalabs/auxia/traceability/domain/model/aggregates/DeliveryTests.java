package com.waqtalabs.auxia.traceability.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryAlreadyRegisteredException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryDateInFutureException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DuplicateDeliveryResourceException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DuplicateEvidenceException;
import com.waqtalabs.auxia.traceability.domain.exceptions.EmptyDeliveryException;
import com.waqtalabs.auxia.traceability.domain.exceptions.EvidenceRequiredException;
import com.waqtalabs.auxia.traceability.domain.exceptions.UnsupportedEvidenceTypeException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.AnchorStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.domain.services.HashingService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryTests {

  private static final LocalDateTime DELIVERED_AT = LocalDateTime.of(2026, 3, 15, 10, 30, 15);
  private static final LocalDateTime RECEIVED_AT = LocalDateTime.of(2026, 3, 15, 18, 0);
  private static final String TRANSACTION = "0x" + "c".repeat(64);
  private static final HashingService SHA_256 =
      new HashingService() {
        @Override
        public HashValue hashRecord(CanonicalDeliveryRecord record) {
          return hashContent(record.toCanonicalJson().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HashValue hashContent(byte[] content) {
          try {
            return HashValue.of(MessageDigest.getInstance("SHA-256").digest(content));
          } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
          }
        }
      };

  private final ResourceId water = new ResourceId(UUID.randomUUID());
  private final ResourceId food = new ResourceId(UUID.randomUUID());
  private final UserId brigadeMember = new UserId(UUID.randomUUID());
  private final Delivery delivery =
      Delivery.initiate(
          new DistributionPlanId(UUID.randomUUID()),
          new ZoneId(UUID.randomUUID()),
          new BrigadeId(UUID.randomUUID()),
          new OrganizationId(UUID.randomUUID()));

  @Test
  void deliveryStartsInitiatedWithoutRegistrationNorHash() {
    assertThat(delivery.status()).isEqualTo(DeliveryStatus.INITIATED);
    assertThat(delivery.registration()).isEmpty();
    assertThat(delivery.integrity()).isEmpty();
  }

  @Test
  void registrationKeepsWhatWasDeliveredAndHashesItsRecord() {
    register(List.of(DeliveryItem.of(water, 300)), List.of(photo("a")));

    assertThat(delivery.status()).isEqualTo(DeliveryStatus.REGISTERED);
    var registration = delivery.registration().orElseThrow();
    assertThat(registration.registeredBy()).isEqualTo(brigadeMember);
    assertThat(registration.deliveredAt()).isEqualTo(DELIVERED_AT);
    assertThat(registration.receivedAt()).isEqualTo(RECEIVED_AT);
    IntegrityRecord integrity = delivery.integrity().orElseThrow();
    assertThat(integrity.status()).isEqualTo(AnchorStatus.PENDING);
    assertThat(integrity.recordHash()).isEqualTo(SHA_256.hashRecord(delivery.canonicalRecord()));
    assertThat(integrity.attempts()).isZero();
  }

  @Test
  void deliveryDateIsKeptToTheSecond() {
    Delivery other =
        Delivery.initiate(
            delivery.distributionPlanId(),
            delivery.zoneId(),
            delivery.brigadeId(),
            delivery.organizationId());

    other.register(
        List.of(DeliveryItem.of(water, 300)),
        List.of(photo("a")),
        brigadeMember,
        DELIVERED_AT.plusNanos(123_456_789),
        RECEIVED_AT,
        SHA_256);

    assertThat(other.registration().orElseThrow().deliveredAt()).isEqualTo(DELIVERED_AT);
  }

  @Test
  void deliveryWithoutEvidenceIsNotRegistered() {
    var items = List.of(DeliveryItem.of(water, 300));

    assertThatThrownBy(() -> register(items, List.of()))
        .isInstanceOf(EvidenceRequiredException.class);
    assertThat(delivery.isInitiated()).isTrue();
  }

  @Test
  void deliveryWithoutResourcesOrWithARepeatedOneIsNotRegistered() {
    var evidences = List.of(photo("a"));
    var repeated = List.of(DeliveryItem.of(water, 100), DeliveryItem.of(water, 200));

    assertThatThrownBy(() -> register(List.of(), evidences))
        .isInstanceOf(EmptyDeliveryException.class);
    assertThatThrownBy(() -> register(repeated, evidences))
        .isInstanceOf(DuplicateDeliveryResourceException.class);
    assertThat(delivery.isInitiated()).isTrue();
  }

  @Test
  void sameFileIsAttachedOnlyOnce() {
    var items = List.of(DeliveryItem.of(water, 300));
    var evidences = List.of(photo("a"), photo("a"));

    assertThatThrownBy(() -> register(items, evidences))
        .isInstanceOf(DuplicateEvidenceException.class);
  }

  @Test
  void deliveryCannotHappenAfterItsRegistration() {
    var items = List.of(DeliveryItem.of(water, 300));
    var evidences = List.of(photo("a"));

    assertThatThrownBy(
            () ->
                delivery.register(
                    items,
                    evidences,
                    brigadeMember,
                    RECEIVED_AT.plusMinutes(1),
                    RECEIVED_AT,
                    SHA_256))
        .isInstanceOf(DeliveryDateInFutureException.class);
  }

  @Test
  void deliveryIsRegisteredOnlyOnce() {
    register(List.of(DeliveryItem.of(water, 300)), List.of(photo("a")));
    var items = List.of(DeliveryItem.of(water, 100));
    var evidences = List.of(photo("b"));

    assertThatThrownBy(() -> register(items, evidences))
        .isInstanceOf(DeliveryAlreadyRegisteredException.class);
  }

  @Test
  void confirmedTransactionCertifiesTheDelivery() {
    register(List.of(DeliveryItem.of(water, 300)), List.of(photo("a")));

    delivery.markSubmitted(TRANSACTION);
    assertThat(delivery.integrity().orElseThrow().transaction()).contains(TRANSACTION);
    assertThat(delivery.status()).isEqualTo(DeliveryStatus.REGISTERED);

    delivery.markAnchored(RECEIVED_AT.plusMinutes(1));

    assertThat(delivery.status()).isEqualTo(DeliveryStatus.CERTIFIED);
    IntegrityRecord integrity = delivery.integrity().orElseThrow();
    assertThat(integrity.status()).isEqualTo(AnchorStatus.ANCHORED);
    assertThat(integrity.anchoredAt()).isEqualTo(RECEIVED_AT.plusMinutes(1));
    assertThat(integrity.attempts()).isEqualTo(1);
    assertThat(delivery.awaitsAnchoring()).isFalse();
  }

  @Test
  void failedAnchoringForgetsItsTransactionAndCountsTheAttempt() {
    register(List.of(DeliveryItem.of(water, 300)), List.of(photo("a")));
    delivery.markSubmitted(TRANSACTION);

    delivery.markAnchoringFailed();

    IntegrityRecord integrity = delivery.integrity().orElseThrow();
    assertThat(integrity.status()).isEqualTo(AnchorStatus.FAILED);
    assertThat(integrity.transaction()).isEmpty();
    assertThat(integrity.attempts()).isEqualTo(1);
    assertThat(delivery.awaitsAnchoring()).isTrue();
  }

  @Test
  void onlyARegisteredDeliveryIsAnchored() {
    assertThatThrownBy(() -> delivery.markSubmitted(TRANSACTION))
        .isInstanceOf(IllegalStateException.class);
    register(List.of(DeliveryItem.of(water, 300)), List.of(photo("a")));
    assertThatThrownBy(() -> delivery.markAnchored(RECEIVED_AT))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void recordWithOtherFileHashesShowsAChangedPhoto() {
    register(
        List.of(DeliveryItem.of(water, 300), DeliveryItem.of(food, 140)), List.of(photo("a")));
    HashValue registered = delivery.integrity().orElseThrow().recordHash();

    HashValue sameFiles =
        SHA_256.hashRecord(delivery.canonicalRecord(List.of(new HashValue("a".repeat(64)))));
    HashValue changedPhoto =
        SHA_256.hashRecord(delivery.canonicalRecord(List.of(new HashValue("f".repeat(64)))));

    assertThat(sameFiles).isEqualTo(registered);
    assertThat(changedPhoto).isNotEqualTo(registered);
  }

  @Test
  void evidenceTypeComesFromTheMediaType() {
    assertThat(EvidenceType.of("image/jpeg")).isEqualTo(EvidenceType.PHOTO);
    assertThat(EvidenceType.of("IMAGE/PNG")).isEqualTo(EvidenceType.PHOTO);
    assertThat(EvidenceType.of("application/pdf; charset=binary"))
        .isEqualTo(EvidenceType.DOCUMENT);
    assertThatThrownBy(() -> EvidenceType.of("text/plain"))
        .isInstanceOf(UnsupportedEvidenceTypeException.class);
    assertThatThrownBy(() -> EvidenceType.of(null))
        .isInstanceOf(UnsupportedEvidenceTypeException.class);
  }

  private void register(List<DeliveryItem> items, List<Evidence> evidences) {
    delivery.register(items, evidences, brigadeMember, DELIVERED_AT, RECEIVED_AT, SHA_256);
  }

  private static Evidence photo(String hashCharacter) {
    return Evidence.attach(
        EvidenceType.PHOTO,
        "https://storage/evidences/" + hashCharacter,
        new HashValue(hashCharacter.repeat(64)),
        DELIVERED_AT,
        null);
  }
}
