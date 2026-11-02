package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/** An evidence file, by its hash. The file stays private in the evidence storage. */
public record EvidenceResource(
    UUID id,
    EvidenceType type,
    @Schema(description = "SHA-256 of the file") String contentHash,
    LocalDateTime capturedAt,
    @Schema(description = "Null if the device had no location") GeoPointResource location) {}
