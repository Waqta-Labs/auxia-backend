package com.waqtalabs.auxia;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Azurite, the emulator of Azure Blob Storage, for the evidence files. It skips the check of the
 * API version, because the Azure SDK can be newer than the emulator. Its account and key are the
 * well-known development ones that Microsoft publishes.
 */
public final class Azurite {

  private static final DockerImageName IMAGE =
      DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0");
  private static final int BLOB_PORT = 10000;
  private static final String ACCOUNT = "devstoreaccount1";
  private static final String KEY =
      "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

  private Azurite() {}

  public static GenericContainer<?> container() {
    return new GenericContainer<>(IMAGE)
        .withCommand("azurite-blob --blobHost 0.0.0.0 --skipApiVersionCheck --loose")
        .withExposedPorts(BLOB_PORT)
        .waitingFor(Wait.forLogMessage(".*Blob service successfully listens.*", 1));
  }

  public static String connectionString(GenericContainer<?> azurite) {
    return connectionString(
        "http://%s:%d/%s"
            .formatted(azurite.getHost(), azurite.getMappedPort(BLOB_PORT), ACCOUNT));
  }

  /** The connection string of the development account at any endpoint. */
  public static String connectionString(String blobEndpoint) {
    return "DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=%s;"
        .formatted(ACCOUNT, KEY, blobEndpoint);
  }
}
