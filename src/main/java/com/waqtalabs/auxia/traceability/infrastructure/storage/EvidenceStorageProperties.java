package com.waqtalabs.auxia.traceability.infrastructure.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Azure Blob Storage account for the evidence files, under {@code auxia.evidence-storage}.
 *
 * @param connectionString connection string of the storage account, or of Azurite locally
 * @param container private container of the files; it is created if it does not exist
 */
@Validated
@ConfigurationProperties("auxia.evidence-storage")
public record EvidenceStorageProperties(
    @NotBlank String connectionString, @DefaultValue("evidences") @NotBlank String container) {}
