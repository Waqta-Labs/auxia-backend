package com.waqtalabs.auxia.emergencymanagement.application.ports;

import java.util.Optional;

/** Keeps the photos and audios of the SOS outside the database, in a private storage. */
public interface SosAttachmentStorage {

  /** Stores a file and returns where it lives. Storing the same name again replaces it. */
  String store(String name, String contentType, byte[] content);

  /** Content of a stored file; empty if it no longer exists. */
  Optional<byte[]> read(String storageUrl);
}
