package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import java.util.Objects;

/**
 * Attaches the photo or the audio of an SOS (A_04).
 *
 * @param contentType media type the phone declared
 * @param sender address of the client, for the limit per sender
 */
public record AttachToCitizenReportCommand(
    CitizenReportId citizenReportId,
    SosAttachmentId attachmentId,
    String contentType,
    byte[] content,
    String sender) {

  public AttachToCitizenReportCommand {
    Objects.requireNonNull(citizenReportId, "citizenReportId");
    Objects.requireNonNull(attachmentId, "attachmentId");
    Objects.requireNonNull(content, "content");
  }
}
