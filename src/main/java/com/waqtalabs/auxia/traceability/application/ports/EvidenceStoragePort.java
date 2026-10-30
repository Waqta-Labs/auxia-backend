package com.waqtalabs.auxia.traceability.application.ports;

import java.util.Optional;

/** Keeps the evidence files of the deliveries outside the database (Azure Blob Storage). */
public interface EvidenceStoragePort {

  /**
   * Stores a file and returns where it lives. Storing the same name again replaces the file, so a
   * registration sent twice leaves one copy.
   */
  String store(String name, String contentType, byte[] content);

  /** Content of a stored file; empty if it no longer exists. */
  Optional<byte[]> read(String storageUrl);
}
