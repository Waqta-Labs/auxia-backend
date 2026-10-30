package com.waqtalabs.auxia.traceability.infrastructure.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.application.ports.EvidenceStoragePort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Evidence files in a private container of Azure Blob Storage. The container is created the first
 * time a file is stored. Any failure of the storage makes it unavailable (503), except a file
 * that no longer exists when it is read.
 */
@Component
class AzureBlobEvidenceStorage implements EvidenceStoragePort {

  static final String SERVICE_NAME = "Evidence storage";

  private static final int NOT_FOUND = 404;

  private final BlobContainerClient container;
  private volatile boolean containerReady;

  AzureBlobEvidenceStorage(BlobContainerClient evidenceContainer) {
    this.container = evidenceContainer;
  }

  @Override
  public String store(String name, String contentType, byte[] content) {
    try {
      if (!containerReady) {
        container.createIfNotExists();
        containerReady = true;
      }
      BlobClient blob = container.getBlobClient(name);
      blob.uploadWithResponse(
          new BlobParallelUploadOptions(BinaryData.fromBytes(content))
              .setHeaders(new BlobHttpHeaders().setContentType(contentType)),
          null,
          null);
      return blob.getBlobUrl();
    } catch (RuntimeException ex) {
      throw new ExternalServiceUnavailableException(SERVICE_NAME, ex);
    }
  }

  /** A URL of another account or container is not a file of this storage. */
  @Override
  public Optional<byte[]> read(String storageUrl) {
    try {
      BlobUrlParts url = BlobUrlParts.parse(storageUrl);
      if (!container.getAccountName().equals(url.getAccountName())
          || !container.getBlobContainerName().equals(url.getBlobContainerName())) {
        return Optional.empty();
      }
      return Optional.of(container.getBlobClient(url.getBlobName()).downloadContent().toBytes());
    } catch (BlobStorageException ex) {
      if (ex.getStatusCode() == NOT_FOUND) {
        return Optional.empty();
      }
      throw new ExternalServiceUnavailableException(SERVICE_NAME, ex);
    } catch (RuntimeException ex) {
      throw new ExternalServiceUnavailableException(SERVICE_NAME, ex);
    }
  }
}
