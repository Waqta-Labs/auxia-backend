package com.waqtalabs.auxia.emergencymanagement.infrastructure.storage;

import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SosAttachmentStorage;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Photos and audios of the SOS in their own private container of the storage account of the
 * evidence ({@code auxia.citizen-reports.attachments-container}). The container is created the
 * first time a file is stored. A failure of the storage makes it unavailable (503).
 */
@Component
class AzureBlobSosAttachmentStorage implements SosAttachmentStorage {

  static final String SERVICE_NAME = "SOS attachment storage";

  private static final int NOT_FOUND = 404;

  private final BlobContainerClient container;
  private volatile boolean containerReady;

  AzureBlobSosAttachmentStorage(
      @Value("${auxia.evidence-storage.connection-string}") String connectionString,
      @Value("${auxia.citizen-reports.attachments-container}") String containerName) {
    this.container =
        new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .httpClient(new JdkHttpClientBuilder().build())
            .buildClient()
            .getBlobContainerClient(containerName);
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

  @Override
  public Optional<byte[]> read(String storageUrl) {
    try {
      String blobName = BlobUrlParts.parse(storageUrl).getBlobName();
      return Optional.of(container.getBlobClient(blobName).downloadContent().toBytes());
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
