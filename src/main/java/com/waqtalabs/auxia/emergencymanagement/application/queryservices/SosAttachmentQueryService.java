package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SosAttachmentStorage;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportAttachmentsQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.SosAttachmentNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.SosAttachment;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.SosAttachmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The photo and audio of an SOS, for the authority that reviews its field report. */
@Service
@Transactional(readOnly = true)
public class SosAttachmentQueryService {

  private final FieldReportRepository fieldReportRepository;
  private final CitizenReportRepository citizenReportRepository;
  private final SosAttachmentRepository attachmentRepository;
  private final SosAttachmentStorage storage;

  public SosAttachmentQueryService(
      FieldReportRepository fieldReportRepository,
      CitizenReportRepository citizenReportRepository,
      SosAttachmentRepository attachmentRepository,
      SosAttachmentStorage storage) {
    this.fieldReportRepository = fieldReportRepository;
    this.citizenReportRepository = citizenReportRepository;
    this.attachmentRepository = attachmentRepository;
    this.storage = storage;
  }

  /** A file of an SOS with its content. */
  public record SosAttachmentFile(SosAttachment attachment, byte[] content) {}

  /**
   * Attachments of the field report; empty when it does not come from an SOS.
   *
   * @throws FieldReportNotFoundException if the field report does not exist
   */
  public List<SosAttachment> attachments(GetFieldReportAttachmentsQuery query) {
    if (fieldReportRepository.findById(query.fieldReportId()).isEmpty()) {
      throw new FieldReportNotFoundException(query.fieldReportId());
    }
    return citizenReportRepository
        .findByFieldReportId(query.fieldReportId())
        .map(CitizenReport::id)
        .map(attachmentRepository::findByCitizenReportId)
        .orElse(List.of());
  }

  /**
   * @throws SosAttachmentNotFoundException if the field report has no such attachment or its
   *     file no longer exists
   */
  public SosAttachmentFile file(GetFieldReportAttachmentsQuery query) {
    SosAttachment attachment =
        attachments(query).stream()
            .filter(candidate -> candidate.id().equals(query.attachmentId()))
            .findFirst()
            .orElseThrow(() -> new SosAttachmentNotFoundException(query.attachmentId()));
    byte[] content =
        storage
            .read(attachment.storageUrl())
            .orElseThrow(() -> new SosAttachmentNotFoundException(query.attachmentId()));
    return new SosAttachmentFile(attachment, content);
  }
}
