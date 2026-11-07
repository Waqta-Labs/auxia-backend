package com.waqtalabs.auxia.shared.infrastructure.storage;

import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.util.Optional;

/**
 * A private container of Azure Blob Storage (or Azurite locally) for files that only signed-in
 * users read, such as the photos of the SOS or the cards of the brigadists. It is created the
 * first time a file is stored. Any failure of the storage makes it unavailable (503), except a
 * file that no longer exists when it is read.
 */
public class PrivateBlobContainer {

  private static final int NOT_FOUND = 404;

  private final BlobContainerClient container;
  private final String serviceName;
  private volatile boolean containerReady;

  /**
   * Building it does not connect, so the application starts even if the storage is not reachable.
   *
   * @param serviceName name of the storage in the errors, such as {@code SOS attachment storage}
   */
  public PrivateBlobContainer(String connectionString, String containerName, String serviceName) {
    this.container =
        new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .httpClient(new JdkHttpClientBuilder().build())
            .buildClient()
            .getBlobContainerClient(containerName);
    this.serviceName = serviceName;
  }

  /** Stores a file and returns its URL. Storing the same name again replaces it. */
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
      throw new ExternalServiceUnavailableException(serviceName, ex);
    }
  }

  /** Content of a stored file; empty if it no longer exists. */
  public Optional<byte[]> read(String storageUrl) {
    try {
      String blobName = BlobUrlParts.parse(storageUrl).getBlobName();
      return Optional.of(container.getBlobClient(blobName).downloadContent().toBytes());
    } catch (BlobStorageException ex) {
      if (ex.getStatusCode() == NOT_FOUND) {
        return Optional.empty();
      }
      throw new ExternalServiceUnavailableException(serviceName, ex);
    } catch (RuntimeException ex) {
      throw new ExternalServiceUnavailableException(serviceName, ex);
    }
  }
}
