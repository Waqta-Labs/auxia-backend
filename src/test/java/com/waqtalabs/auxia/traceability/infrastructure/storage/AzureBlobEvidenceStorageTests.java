package com.waqtalabs.auxia.traceability.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azure.storage.blob.BlobContainerClient;
import com.waqtalabs.auxia.Azurite;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The adapter against Azurite, the emulator of Azure Blob Storage. */
@Testcontainers
class AzureBlobEvidenceStorageTests {

  @Container private static final GenericContainer<?> AZURITE = Azurite.container();

  private static final byte[] PHOTO = "foto de la entrega".getBytes(StandardCharsets.UTF_8);

  private final String container = "evidences-" + UUID.randomUUID();
  private final BlobContainerClient client = client(Azurite.connectionString(AZURITE), container);
  private final AzureBlobEvidenceStorage storage = new AzureBlobEvidenceStorage(client);

  @Test
  void storedFileIsReadBackFromItsUrlWithItsMediaType() {
    String url = storage.store("delivery/photo-hash", "image/jpeg", PHOTO);

    assertThat(url).contains("/" + container + "/");
    assertThat(storage.read(url)).hasValueSatisfying(read -> assertThat(read).isEqualTo(PHOTO));
    assertThat(client.getBlobClient("delivery/photo-hash").getProperties().getContentType())
        .isEqualTo("image/jpeg");
    assertThat(client.exists()).isTrue();
  }

  @Test
  void sameNameKeepsOneCopyWithTheLastContent() {
    byte[] first = "primera".getBytes(StandardCharsets.UTF_8);
    storage.store("delivery/photo-hash", "image/jpeg", first);
    String url = storage.store("delivery/photo-hash", "image/jpeg", PHOTO);

    assertThat(storage.read(url)).hasValueSatisfying(read -> assertThat(read).isEqualTo(PHOTO));
    assertThat(client.listBlobs()).hasSize(1);
  }

  @Test
  void missingFileOrUrlOfAnotherContainerIsEmpty() {
    String url = storage.store("delivery/photo-hash", "image/jpeg", PHOTO);
    client.getBlobClient("delivery/photo-hash").delete();

    assertThat(storage.read(url)).isEmpty();
    assertThat(storage.read("https://elsewhere.test/evidences/photo")).isEmpty();
  }

  @Test
  void unreachableStorageIsUnavailable() {
    var unreachable =
        new AzureBlobEvidenceStorage(
            client(Azurite.connectionString("http://localhost:1/devstoreaccount1"), container));

    assertThatThrownBy(() -> unreachable.store("delivery/photo-hash", "image/jpeg", PHOTO))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  private static BlobContainerClient client(String connectionString, String container) {
    return new EvidenceStorageConfiguration()
        .evidenceContainer(new EvidenceStorageProperties(connectionString, container));
  }
}
