package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.AnchorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Hash of the record of the delivery and its anchoring in the blockchain (US-16). */
public record IntegrityResource(
    @Schema(description = "SHA-256 of the record, without personal data") String recordHash,
    AnchorStatus anchorStatus,
    @Schema(description = "Transaction that carries the hash; null until it is sent")
        String transactionHash,
    @Schema(description = "When the transaction was confirmed") LocalDateTime anchoredAt,
    @Schema(description = "Transactions sent so far") int attempts) {}
