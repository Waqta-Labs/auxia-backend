package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.SosAttachment;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AttachmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Quick photo or 5-second audio of an SOS (A_04)")
public record SosAttachmentResource(
    UUID attachmentId,
    @Schema(example = "PHOTO") AttachmentType type,
    @Schema(example = "image/jpeg") String contentType,
    @Schema(description = "Bytes of the file", example = "245760") long size) {

  public static SosAttachmentResource from(SosAttachment attachment) {
    return new SosAttachmentResource(
        attachment.id().value(), attachment.type(), attachment.contentType(), attachment.size());
  }
}
