package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InvalidSosAttachmentException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AttachmentType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Quick photo or 5-second audio of an SOS (A_04). The phone generates its identifier, so a retry
 * is recognized. The file itself lives in the storage; only the authority reads it.
 *
 * @param contentType normalized media type, such as {@code image/jpeg}
 * @param size bytes of the file
 * @param storageUrl where the file is stored
 */
public record SosAttachment(
    SosAttachmentId id,
    CitizenReportId citizenReportId,
    AttachmentType type,
    String contentType,
    long size,
    String storageUrl,
    LocalDateTime receivedAt) {

  public SosAttachment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(citizenReportId, "citizenReportId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(storageUrl, "storageUrl");
    Objects.requireNonNull(receivedAt, "receivedAt");
  }

  /**
   * The kind of a file the citizen sent, if it can be attached to an SOS.
   *
   * @throws InvalidSosAttachmentException if its media type is not admitted, or it is empty or
   *     larger than its kind admits
   */
  public static AttachmentType typeOf(String contentType, long size) {
    AttachmentType type =
        AttachmentType.of(contentType)
            .orElseThrow(
                () -> new InvalidSosAttachmentException("unsupported type " + contentType));
    if (size <= 0 || size > type.maxBytes()) {
      throw new InvalidSosAttachmentException(
          type + " of " + size + " bytes; up to " + type.maxMegabytes() + " MB");
    }
    return type;
  }
}
