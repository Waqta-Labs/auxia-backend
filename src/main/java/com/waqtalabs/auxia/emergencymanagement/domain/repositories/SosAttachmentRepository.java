package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.SosAttachment;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import java.util.List;
import java.util.Optional;

public interface SosAttachmentRepository {

  SosAttachment save(SosAttachment attachment);

  Optional<SosAttachment> findById(SosAttachmentId id);

  /** Attachments of the SOS, photo first. */
  List<SosAttachment> findByCitizenReportId(CitizenReportId citizenReportId);
}
