package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.AttachToCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SosAttachmentStorage;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SosRateLimiter;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.CitizenReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InvalidSosAttachmentException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.SosAttachment;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AttachmentType;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.SosAttachmentRepository;
import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores the photo or the audio of an SOS. A retry with the same identifier returns the stored
 * file without counting it for the limit of the sender. An SOS has at most one of each kind.
 */
@Service
public class AttachToCitizenReportCommandHandler {

  private final CitizenReportRepository citizenReportRepository;
  private final SosAttachmentRepository attachmentRepository;
  private final SosAttachmentStorage storage;
  private final SosRateLimiter rateLimiter;
  private final Clock clock;

  public AttachToCitizenReportCommandHandler(
      CitizenReportRepository citizenReportRepository,
      SosAttachmentRepository attachmentRepository,
      SosAttachmentStorage storage,
      SosRateLimiter rateLimiter,
      Clock clock) {
    this.citizenReportRepository = citizenReportRepository;
    this.attachmentRepository = attachmentRepository;
    this.storage = storage;
    this.rateLimiter = rateLimiter;
    this.clock = clock;
  }

  /**
   * The attachment, and whether this request stored it.
   *
   * @param created {@code false} when an earlier request already stored it
   */
  public record Attachment(SosAttachment attachment, boolean created) {}

  /**
   * @throws CitizenReportNotFoundException if the SOS has not arrived yet
   * @throws InvalidSosAttachmentException if the file is not admitted, or the SOS already has
   *     one of its kind
   * @throws RateLimitExceededException if the sender reached the limit
   */
  @Transactional
  public Attachment handle(AttachToCitizenReportCommand command) {
    Optional<SosAttachment> stored = attachmentRepository.findById(command.attachmentId());
    if (stored.isPresent()) {
      if (!stored.get().citizenReportId().equals(command.citizenReportId())) {
        throw new InvalidSosAttachmentException("the id belongs to another SOS");
      }
      return new Attachment(stored.get(), false);
    }
    if (citizenReportRepository.findById(command.citizenReportId()).isEmpty()) {
      throw new CitizenReportNotFoundException(command.citizenReportId());
    }
    AttachmentType type = SosAttachment.typeOf(command.contentType(), command.content().length);
    boolean alreadyHasOne =
        attachmentRepository.findByCitizenReportId(command.citizenReportId()).stream()
            .anyMatch(attachment -> attachment.type() == type);
    if (alreadyHasOne) {
      throw new InvalidSosAttachmentException("the SOS already has a " + type);
    }
    rateLimiter.acquire(command.sender(), false);

    String contentType = AttachmentType.normalize(command.contentType());
    String storageUrl =
        storage.store(
            command.citizenReportId().value() + "/" + command.attachmentId().value(),
            contentType,
            command.content());
    SosAttachment attachment =
        new SosAttachment(
            command.attachmentId(),
            command.citizenReportId(),
            type,
            contentType,
            command.content().length,
            storageUrl,
            LocalDateTime.now(clock));
    return new Attachment(attachmentRepository.save(attachment), true);
  }
}
