package com.waqtalabs.auxia.traceability.application;

import com.waqtalabs.auxia.traceability.application.ports.EvidenceStoragePort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Test double of the evidence storage: files by URL, which can be replaced or deleted. */
public class InMemoryEvidenceStorage implements EvidenceStoragePort {

  static final String BASE_URL = "https://storage.test/evidences/";

  private final Map<String, byte[]> files = new LinkedHashMap<>();

  @Override
  public String store(String name, String contentType, byte[] content) {
    String url = BASE_URL + name;
    files.put(url, content.clone());
    return url;
  }

  @Override
  public Optional<byte[]> read(String storageUrl) {
    return Optional.ofNullable(files.get(storageUrl)).map(byte[]::clone);
  }

  public Map<String, byte[]> files() {
    return Map.copyOf(files);
  }

  /** Changes a stored file, as someone with access to the storage could. */
  public void replace(String storageUrl, byte[] content) {
    files.put(storageUrl, content.clone());
  }

  public void delete(String storageUrl) {
    files.remove(storageUrl);
  }
}
