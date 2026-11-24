package com.waqtalabs.auxia.emergencymanagement.infrastructure.storage;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SosAttachmentStorage;
import com.waqtalabs.auxia.shared.infrastructure.storage.PrivateBlobContainer;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Photos and audios of the SOS in their own private container of the storage account of the
 * evidence ({@code auxia.citizen-reports.attachments-container}).
 */
@Component
class AzureBlobSosAttachmentStorage implements SosAttachmentStorage {

  private final PrivateBlobContainer container;

  AzureBlobSosAttachmentStorage(
      @Value("${auxia.evidence-storage.connection-string}") String connectionString,
      @Value("${auxia.citizen-reports.attachments-container}") String containerName) {
    this.container =
        new PrivateBlobContainer(connectionString, containerName, "SOS attachment storage");
  }

  @Override
  public String store(String name, String contentType, byte[] content) {
    return container.store(name, contentType, content);
  }

  @Override
  public Optional<byte[]> read(String storageUrl) {
    return container.read(storageUrl);
  }
}
