package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Result of the verification of a delivery (US-17). It shows no data of the delivery. */
public record VerificationResource(
    UUID deliveryId,
    @Schema(description = "Hash computed again from the data and files stored now")
        String recordHash,
    @Schema(description = "Hash stored in the smart contract; null if not anchored yet")
        String anchoredHash,
    @Schema(description = "Transaction that anchored it; null if not anchored yet")
        String transactionHash,
    @Schema(description = "Both hashes match: the delivery was not altered") boolean verified) {}
