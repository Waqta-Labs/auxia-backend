package com.waqtalabs.auxia.traceability.application.queryservices;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.application.ports.BlockchainLedgerPort;
import com.waqtalabs.auxia.traceability.application.ports.EvidenceStoragePort;
import com.waqtalabs.auxia.traceability.application.queries.VerifyDeliveryQuery;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryNotFoundException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.VerificationResult;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import com.waqtalabs.auxia.traceability.domain.services.HashingService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies a delivery (US-17). It computes the hash again from what is stored now: the
 * quantities in PostgreSQL and the evidence files read again from the storage. Then it compares
 * that hash with the one in the {@code DeliveryRegistry} contract. If someone changes a quantity
 * in the database, or replaces a photo, the hashes no longer match.
 */
@Service
public class VerifyDeliveryQueryHandler {

  /** Stands for a file that is no longer in the storage: it never matches a real hash. */
  private static final HashValue MISSING_FILE = new HashValue("0".repeat(64));

  private final DeliveryRepository deliveryRepository;
  private final EvidenceStoragePort evidenceStorage;
  private final BlockchainLedgerPort ledger;
  private final HashingService hashing;

  public VerifyDeliveryQueryHandler(
      DeliveryRepository deliveryRepository,
      EvidenceStoragePort evidenceStorage,
      BlockchainLedgerPort ledger,
      HashingService hashing) {
    this.deliveryRepository = deliveryRepository;
    this.evidenceStorage = evidenceStorage;
    this.ledger = ledger;
    this.hashing = hashing;
  }

  /**
   * @throws DeliveryNotFoundException if no registered delivery has that identifier
   * @throws ExternalServiceUnavailableException if the blockchain does not answer
   */
  @Transactional(readOnly = true)
  public VerificationResult handle(VerifyDeliveryQuery query) {
    Delivery delivery =
        deliveryRepository
            .findById(query.deliveryId())
            .filter(found -> !found.isInitiated())
            .orElseThrow(() -> new DeliveryNotFoundException(query.deliveryId()));
    List<HashValue> fileHashes = delivery.evidences().stream().map(this::hashOfStoredFile).toList();
    HashValue recordHash = hashing.hashRecord(delivery.canonicalRecord(fileHashes));
    Optional<HashValue> anchoredHash = ledger.deliveryHash(delivery.id());
    Optional<String> transaction =
        delivery
            .integrity()
            .filter(IntegrityRecord::isAnchored)
            .flatMap(IntegrityRecord::transaction);
    return new VerificationResult(delivery.id(), recordHash, anchoredHash, transaction);
  }

  private HashValue hashOfStoredFile(Evidence evidence) {
    return evidenceStorage
        .read(evidence.storageUrl())
        .map(hashing::hashContent)
        .orElse(MISSING_FILE);
  }
}
