package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/**
 * An SOS carries one photo of up to 10 MB and one audio of up to 5 MB (A_04), in the media types
 * of {@code AttachmentType}. Anything else is rejected, and the app discards it.
 */
public class InvalidSosAttachmentException extends RuleViolationException {

  public InvalidSosAttachmentException(String reason) {
    super("Invalid SOS attachment: " + reason, reason);
  }
}
