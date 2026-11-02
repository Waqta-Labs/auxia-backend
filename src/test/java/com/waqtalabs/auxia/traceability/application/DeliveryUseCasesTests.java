package com.waqtalabs.auxia.traceability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.waqtalabs.auxia.shared.application.exceptions.ActionNotAllowedException;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import com.waqtalabs.auxia.traceability.application.commandhandlers.AnchorDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commandhandlers.InitiateDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commandhandlers.RegisterDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commandhandlers.RegisterDeliveryCommandHandler.RegisteredDelivery;
import com.waqtalabs.auxia.traceability.application.commands.AnchorDeliveryCommand;
import com.waqtalabs.auxia.traceability.application.commands.InitiateDeliveryCommand;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand.DeliveredQuantity;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand.EvidenceFile;
import com.waqtalabs.auxia.traceability.application.eventhandlers.PendingAnchoringScheduler;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveriesByZoneIdQuery;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveryByIdQuery;
import com.waqtalabs.auxia.traceability.application.queries.VerifyDeliveryQuery;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryQueryService;
import com.waqtalabs.auxia.traceability.application.queryservices.VerifyDeliveryQueryHandler;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryAnchoringFailed;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryCertified;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryConfirmed;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryInitiated;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveredBeyondPlanException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryNotFoundException;
import com.waqtalabs.auxia.traceability.domain.exceptions.EvidenceRequiredException;
import com.waqtalabs.auxia.traceability.domain.exceptions.UnsupportedEvidenceTypeException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.AnchorStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.VerificationResult;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-15T23:00:00Z"), ZoneOffset.ofHours(-5));
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  private static final LocalDateTime DELIVERED_AT = LocalDateTime.of(2026, 3, 15, 10, 30);
  private static final byte[] PHOTO = "foto de la entrega".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ACTA = "%PDF acta de entrega".getBytes(StandardCharsets.UTF_8);

  private final InMemoryDeliveryRepository deliveries = new InMemoryDeliveryRepository();
  private final InMemoryEvidenceStorage storage = new InMemoryEvidenceStorage();
  private final FakeBlockchainLedger ledger = new FakeBlockchainLedger();
  private final Sha256 hashing = new Sha256();
  private final List<DomainEvent> events = new ArrayList<>();
  private final Set<UserId> brigadeMembers = new HashSet<>();
  private final Map<DistributionPlanId, Map<ResourceId, Integer>> plans = new HashMap<>();

  private final InitiateDeliveryCommandHandler initiate =
      new InitiateDeliveryCommandHandler(deliveries, events::add);
  private final RegisterDeliveryCommandHandler register =
      new RegisterDeliveryCommandHandler(
          deliveries,
          plan -> plans.getOrDefault(plan, Map.of()),
          storage,
          brigadeMembers::contains,
          hashing,
          events::add,
          CLOCK);
  private final AnchorDeliveryCommandHandler anchor =
      new AnchorDeliveryCommandHandler(deliveries, ledger, events::add, CLOCK);
  private final DeliveryQueryService queries = new DeliveryQueryService(deliveries);
  private final VerifyDeliveryQueryHandler verify =
      new VerifyDeliveryQueryHandler(deliveries, storage, ledger, hashing);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final DistributionPlanId plan = new DistributionPlanId(UUID.randomUUID());
  private final ZoneId zone = new ZoneId(UUID.randomUUID());
  private final BrigadeId brigade = new BrigadeId(UUID.randomUUID());
  private final UserId member = new UserId(UUID.randomUUID());
  private final ResourceId water = new ResourceId(UUID.randomUUID());
  private final ResourceId food = new ResourceId(UUID.randomUUID());

  DeliveryUseCasesTests() {
    brigadeMembers.add(member);
    plans.put(plan, Map.of(water, 500, food, 300));
  }

  @Test
  void assignedBrigadeOpensOneDeliveryPerPlan() {
    Delivery opened = initiate();

    assertThat(opened.status()).isEqualTo(DeliveryStatus.INITIATED);
    assertThat(opened.brigadeId()).isEqualTo(brigade);
    assertThat(initiate().id()).isEqualTo(opened.id());
    assertThat(events).containsExactly(new DeliveryInitiated(opened.id(), plan, zone, brigade));
  }

  @Test
  void registrationStoresEachFileByItsHashAndAnnouncesTheDelivery() {
    Delivery opened = initiate();

    var command = command(opened, List.of(photo(), acta()), new DeliveredQuantity(water, 300));

    RegisteredDelivery registered = register.handle(command);

    Delivery delivery = registered.delivery();
    assertThat(registered.registeredNow()).isTrue();
    assertThat(delivery.status()).isEqualTo(DeliveryStatus.REGISTERED);
    assertThat(delivery.registration().orElseThrow().receivedAt()).isEqualTo(NOW);
    assertThat(delivery.evidences())
        .extracting(Evidence::type, Evidence::contentHash, Evidence::capturedAt)
        .containsExactly(
            tuple(EvidenceType.PHOTO, hashing.hashContent(PHOTO), DELIVERED_AT),
            tuple(EvidenceType.DOCUMENT, hashing.hashContent(ACTA), DELIVERED_AT));
    assertThat(delivery.evidences().getFirst().location()).contains(new GeoPoint(-11.94, -76.7));
    assertThat(storage.files())
        .containsKey(
            InMemoryEvidenceStorage.BASE_URL
                + opened.id().value()
                + "/"
                + hashing.hashContent(PHOTO).value());
    assertThat(events)
        .filteredOn(DeliveryConfirmed.class::isInstance)
        .singleElement()
        .isEqualTo(
            new DeliveryConfirmed(
                opened.id(),
                plan,
                zone,
                brigade,
                organization,
                List.of(new CanonicalDeliveryRecord.Line(water, 300)),
                DELIVERED_AT));
  }

  @Test
  void registrationSentAgainReturnsTheDeliveryAsItIs() {
    Delivery opened = initiate();
    register.handle(command(opened, List.of(photo()), new DeliveredQuantity(water, 300)));
    events.clear();

    RegisteredDelivery again =
        register.handle(command(opened, List.of(acta()), new DeliveredQuantity(water, 100)));

    assertThat(again.registeredNow()).isFalse();
    assertThat(again.delivery().items().getFirst().quantity()).isEqualTo(300);
    assertThat(storage.files()).hasSize(1);
    assertThat(events).isEmpty();
  }

  @Test
  void deliveryWithoutEvidenceWithContentIsNotRegistered() {
    Delivery opened = initiate();
    var empty = new EvidenceFile("image/jpeg", new byte[0]);

    assertThatThrownBy(
            () ->
                register.handle(
                    command(opened, List.of(empty), new DeliveredQuantity(water, 300))))
        .isInstanceOf(EvidenceRequiredException.class);
    assertThat(deliveries.findById(opened.id()).orElseThrow().isInitiated()).isTrue();
    assertThat(storage.files()).isEmpty();
  }

  @Test
  void evidenceOfAnotherTypeIsRejectedBeforeStoringAnything() {
    Delivery opened = initiate();
    var text = new EvidenceFile("text/plain", PHOTO);

    assertThatThrownBy(
            () ->
                register.handle(
                    command(opened, List.of(photo(), text), new DeliveredQuantity(water, 300))))
        .isInstanceOf(UnsupportedEvidenceTypeException.class);
    assertThat(storage.files()).isEmpty();
  }

  @Test
  void deliveryGivesAtMostWhatThePlanHolds() {
    Delivery opened = initiate();
    var other = new ResourceId(UUID.randomUUID());

    assertThatThrownBy(
            () ->
                register.handle(
                    command(opened, List.of(photo()), new DeliveredQuantity(water, 501))))
        .isInstanceOf(DeliveredBeyondPlanException.class);
    assertThatThrownBy(
            () ->
                register.handle(command(opened, List.of(photo()), new DeliveredQuantity(other, 1))))
        .isInstanceOf(DeliveredBeyondPlanException.class);
    assertThat(storage.files()).isEmpty();
  }

  @Test
  void onlyAnActiveBrigadeMemberOrAuthorityOfTheOrganizationRegisters() {
    Delivery opened = initiate();
    var items = new DeliveredQuantity(water, 300);
    var otherOrganization =
        new RegisterDeliveryCommand(
            opened.id(),
            new OrganizationId(UUID.randomUUID()),
            member,
            DELIVERED_AT,
            List.of(items),
            Optional.empty(),
            List.of(photo()));

    assertThatThrownBy(() -> register.handle(otherOrganization))
        .isInstanceOf(DeliveryNotFoundException.class);
    brigadeMembers.clear();
    assertThatThrownBy(() -> register.handle(command(opened, List.of(photo()), items)))
        .isInstanceOf(ActionNotAllowedException.class);
  }

  @Test
  void confirmedTransactionCertifiesTheDelivery() {
    Delivery registered = registered();

    Delivery anchored = anchor.handle(new AnchorDeliveryCommand(registered.id()));

    assertThat(anchored.status()).isEqualTo(DeliveryStatus.CERTIFIED);
    var integrity = anchored.integrity().orElseThrow();
    assertThat(integrity.anchoredAt()).isEqualTo(NOW);
    assertThat(integrity.transaction()).contains(ledger.sentTransactions().getFirst());
    assertThat(ledger.deliveryHash(registered.id())).contains(integrity.recordHash());
    assertThat(events)
        .filteredOn(DeliveryCertified.class::isInstance)
        .singleElement()
        .isEqualTo(
            new DeliveryCertified(registered.id(), zone, ledger.sentTransactions().getFirst()));
    assertThat(anchor.handle(new AnchorDeliveryCommand(registered.id())).status())
        .isEqualTo(DeliveryStatus.CERTIFIED);
    assertThat(ledger.sentTransactions()).hasSize(1);
  }

  @Test
  void unconfirmedTransactionIsCheckedAgainInsteadOfSendingAnother() {
    Delivery registered = registered();
    ledger.willStayPending();

    Delivery waiting = anchor.handle(new AnchorDeliveryCommand(registered.id()));
    assertThat(waiting.status()).isEqualTo(DeliveryStatus.REGISTERED);
    assertThat(waiting.integrity().orElseThrow().transaction()).isPresent();

    ledger.willConfirm();
    Delivery anchored = anchor.handle(new AnchorDeliveryCommand(registered.id()));

    assertThat(anchored.status()).isEqualTo(DeliveryStatus.CERTIFIED);
    assertThat(ledger.sentTransactions()).hasSize(1);
  }

  @Test
  void rejectedTransactionOrSilentNetworkLeavesTheAnchoringFailed() {
    Delivery registered = registered();
    ledger.willRevert();

    Delivery reverted = anchor.handle(new AnchorDeliveryCommand(registered.id()));
    assertThat(reverted.integrity().orElseThrow().status()).isEqualTo(AnchorStatus.FAILED);
    assertThat(reverted.integrity().orElseThrow().attempts()).isEqualTo(1);

    ledger.willConfirm();
    ledger.becomeUnavailable();
    Delivery unanswered = anchor.handle(new AnchorDeliveryCommand(registered.id()));
    assertThat(unanswered.integrity().orElseThrow().status()).isEqualTo(AnchorStatus.FAILED);
    assertThat(events).filteredOn(DeliveryAnchoringFailed.class::isInstance).hasSize(2);

    ledger.becomeAvailable();
    assertThat(anchor.handle(new AnchorDeliveryCommand(registered.id())).status())
        .isEqualTo(DeliveryStatus.CERTIFIED);
  }

  @Test
  void hashAlreadyInTheContractIsNotSentAgain() {
    Delivery registered = registered();
    ledger.store(registered.id(), new HashValue("f".repeat(64)));

    Delivery delivery = anchor.handle(new AnchorDeliveryCommand(registered.id()));

    assertThat(delivery.integrity().orElseThrow().status()).isEqualTo(AnchorStatus.FAILED);
    assertThat(ledger.sentTransactions()).isEmpty();
  }

  @Test
  void schedulerRetriesOnlyTheDeliveriesRegisteredBeforeTheLastInterval() {
    Delivery registered = registered();
    ledger.becomeUnavailable();
    anchor.handle(new AnchorDeliveryCommand(registered.id()));
    ledger.becomeAvailable();
    var later = Clock.offset(CLOCK, Duration.ofMinutes(10));

    new PendingAnchoringScheduler(deliveries, anchor, Duration.ofMinutes(15), 10, later)
        .retryPendingAnchoring();
    assertThat(deliveries.findById(registered.id()).orElseThrow().status())
        .isEqualTo(DeliveryStatus.REGISTERED);

    new PendingAnchoringScheduler(deliveries, anchor, Duration.ofMinutes(5), 10, later)
        .retryPendingAnchoring();
    assertThat(deliveries.findById(registered.id()).orElseThrow().status())
        .isEqualTo(DeliveryStatus.CERTIFIED);
  }

  @Test
  void schedulerGivesUpAfterTheMaximumAttempts() {
    Delivery registered = registered();
    ledger.willRevert();
    anchor.handle(new AnchorDeliveryCommand(registered.id()));
    ledger.willConfirm();
    var later = Clock.offset(CLOCK, Duration.ofMinutes(10));

    new PendingAnchoringScheduler(deliveries, anchor, Duration.ofMinutes(5), 1, later)
        .retryPendingAnchoring();

    assertThat(deliveries.findById(registered.id()).orElseThrow().status())
        .isEqualTo(DeliveryStatus.REGISTERED);
  }

  @Test
  void untouchedDeliveryIsVerified() {
    Delivery delivery = anchor.handle(new AnchorDeliveryCommand(registered().id()));

    VerificationResult result = verify.handle(new VerifyDeliveryQuery(delivery.id()));

    assertThat(result.verified()).isTrue();
    var integrity = delivery.integrity().orElseThrow();
    assertThat(result.recordHash()).isEqualTo(integrity.recordHash());
    assertThat(result.anchoredHash()).contains(result.recordHash());
    assertThat(result.transactionHash()).isEqualTo(integrity.transaction());
  }

  @Test
  void replacedOrMissingPhotoIsDetected() {
    Delivery delivery = anchor.handle(new AnchorDeliveryCommand(registered().id()));
    String photoUrl = delivery.evidences().getFirst().storageUrl();

    storage.replace(photoUrl, "otra foto".getBytes(StandardCharsets.UTF_8));
    assertThat(verify.handle(new VerifyDeliveryQuery(delivery.id())).verified()).isFalse();

    storage.delete(photoUrl);
    assertThat(verify.handle(new VerifyDeliveryQuery(delivery.id())).verified()).isFalse();
  }

  @Test
  void deliveryNotAnchoredYetIsNotVerified() {
    Delivery delivery = registered();

    VerificationResult result = verify.handle(new VerifyDeliveryQuery(delivery.id()));

    assertThat(result.verified()).isFalse();
    assertThat(result.anchoredHash()).isEmpty();
    assertThat(result.transactionHash()).isEmpty();
  }

  @Test
  void onlyARegisteredDeliveryCanBeVerified() {
    Delivery opened = initiate();
    var unknown = new DeliveryId(UUID.randomUUID());

    assertThatThrownBy(() -> verify.handle(new VerifyDeliveryQuery(opened.id())))
        .isInstanceOf(DeliveryNotFoundException.class);
    assertThatThrownBy(() -> verify.handle(new VerifyDeliveryQuery(unknown)))
        .isInstanceOf(DeliveryNotFoundException.class);
  }

  @Test
  void organizationReadsOnlyItsDeliveries() {
    Delivery opened = initiate();
    var other = new OrganizationId(UUID.randomUUID());

    assertThat(queries.handle(new GetDeliveryByIdQuery(opened.id(), organization)))
        .isEqualTo(opened);
    assertThat(queries.handle(new GetDeliveriesByZoneIdQuery(zone, organization)))
        .containsExactly(opened);
    assertThat(queries.handle(new GetDeliveriesByZoneIdQuery(zone, other))).isEmpty();
    assertThatThrownBy(() -> queries.handle(new GetDeliveryByIdQuery(opened.id(), other)))
        .isInstanceOf(DeliveryNotFoundException.class);
  }

  private Delivery initiate() {
    return initiate.handle(new InitiateDeliveryCommand(plan, zone, brigade, organization));
  }

  private Delivery registered() {
    Delivery opened = initiate();
    return register
        .handle(command(opened, List.of(photo()), new DeliveredQuantity(water, 300)))
        .delivery();
  }

  private RegisterDeliveryCommand command(
      Delivery delivery, List<EvidenceFile> evidences, DeliveredQuantity... items) {
    return new RegisterDeliveryCommand(
        delivery.id(),
        organization,
        member,
        DELIVERED_AT,
        List.of(items),
        Optional.of(new GeoPoint(-11.94, -76.7)),
        evidences);
  }

  private static EvidenceFile photo() {
    return new EvidenceFile("image/jpeg", PHOTO);
  }

  private static EvidenceFile acta() {
    return new EvidenceFile("application/pdf", ACTA);
  }
}
