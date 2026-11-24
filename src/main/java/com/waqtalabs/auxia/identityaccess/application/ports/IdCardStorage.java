package com.waqtalabs.auxia.identityaccess.application.ports;

import java.util.Optional;

/** Keeps the photos of the institutional cards of the brigadists in a private storage. */
public interface IdCardStorage {

  /** Stores a file and returns where it lives. Storing the same name again replaces it. */
  String store(String name, String contentType, byte[] content);

  /** Content of a stored file; empty if it no longer exists. */
  Optional<byte[]> read(String storageUrl);
}
