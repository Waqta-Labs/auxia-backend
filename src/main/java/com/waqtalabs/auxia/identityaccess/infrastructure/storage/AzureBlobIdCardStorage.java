package com.waqtalabs.auxia.identityaccess.infrastructure.storage;

import com.waqtalabs.auxia.identityaccess.application.ports.IdCardStorage;
import com.waqtalabs.auxia.shared.infrastructure.storage.PrivateBlobContainer;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Photos of the institutional cards of the brigadists (A_02) in their own private container of
 * the storage account of the evidence ({@code auxia.brigade-registrations.id-cards-container}).
 */
@Component
class AzureBlobIdCardStorage implements IdCardStorage {

  private final PrivateBlobContainer container;

  AzureBlobIdCardStorage(
      @Value("${auxia.evidence-storage.connection-string}") String connectionString,
      @Value("${auxia.brigade-registrations.id-cards-container}") String containerName) {
    this.container = new PrivateBlobContainer(connectionString, containerName, "ID card storage");
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
