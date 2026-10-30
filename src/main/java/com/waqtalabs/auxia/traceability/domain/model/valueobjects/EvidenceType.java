package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import com.waqtalabs.auxia.traceability.domain.exceptions.UnsupportedEvidenceTypeException;
import java.util.Locale;
import java.util.Set;

/** Kind of evidence file. */
public enum EvidenceType {
  PHOTO,
  DOCUMENT;

  private static final Set<String> PHOTOS =
      Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
  private static final String PDF = "application/pdf";

  /**
   * The kind of a file by its media type.
   *
   * @throws UnsupportedEvidenceTypeException if it is not a JPEG, PNG, WebP or HEIC photo, or a
   *     PDF document
   */
  public static EvidenceType of(String contentType) {
    String mediaType =
        contentType == null ? "" : contentType.split(";")[0].strip().toLowerCase(Locale.ROOT);
    if (PHOTOS.contains(mediaType)) {
      return PHOTO;
    }
    if (PDF.equals(mediaType)) {
      return DOCUMENT;
    }
    throw new UnsupportedEvidenceTypeException(contentType);
  }
}
