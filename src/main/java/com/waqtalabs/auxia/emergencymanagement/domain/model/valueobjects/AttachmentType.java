package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Kind of file an SOS carries (A_04): a quick photo or a 5-second audio. Each kind admits some
 * media types and a maximum size.
 */
public enum AttachmentType {
  PHOTO(10, Set.of("image/jpeg", "image/png", "image/webp", "image/heic")),
  AUDIO(5, Set.of("audio/mp4", "audio/aac", "audio/mpeg", "audio/ogg", "audio/wav"));

  /** Names that some phones give to the same media types. */
  private static final Map<String, String> ALIASES =
      Map.of(
          "image/jpg", "image/jpeg",
          "audio/m4a", "audio/mp4",
          "audio/x-m4a", "audio/mp4",
          "audio/x-wav", "audio/wav",
          "audio/mp3", "audio/mpeg");

  private final int maxMegabytes;
  private final Set<String> contentTypes;

  AttachmentType(int maxMegabytes, Set<String> contentTypes) {
    this.maxMegabytes = maxMegabytes;
    this.contentTypes = contentTypes;
  }

  /**
   * The kind of a media type, ignoring its parameters and case.
   *
   * @return empty when no kind admits it
   */
  public static Optional<AttachmentType> of(String contentType) {
    String normalized = normalize(contentType);
    for (AttachmentType type : values()) {
      if (type.contentTypes.contains(normalized)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  /** {@code Image/JPG; q=1} becomes {@code image/jpeg}. */
  public static String normalize(String contentType) {
    if (contentType == null) {
      return "";
    }
    String bare = contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
    return ALIASES.getOrDefault(bare, bare);
  }

  public int maxMegabytes() {
    return maxMegabytes;
  }

  public long maxBytes() {
    return maxMegabytes * 1024L * 1024L;
  }
}
