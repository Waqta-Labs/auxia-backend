package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.SosAttachment;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.SosAttachmentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaSosAttachmentRepository implements SosAttachmentRepository {

  private final SpringDataSosAttachmentRepository repository;

  JpaSosAttachmentRepository(SpringDataSosAttachmentRepository repository) {
    this.repository = repository;
  }

  @Override
  public SosAttachment save(SosAttachment attachment) {
    var entity = new SosAttachmentJpaEntity();
    entity.id = attachment.id().value();
    entity.citizenReportId = attachment.citizenReportId().value();
    entity.type = attachment.type();
    entity.contentType = attachment.contentType();
    entity.size = attachment.size();
    entity.storageUrl = attachment.storageUrl();
    entity.receivedAt = attachment.receivedAt();
    return toDomain(repository.save(entity));
  }

  @Override
  public Optional<SosAttachment> findById(SosAttachmentId id) {
    return repository.findById(id.value()).map(JpaSosAttachmentRepository::toDomain);
  }

  /** {@code PHOTO} sorts after {@code AUDIO}, so descending puts the photo first. */
  @Override
  public List<SosAttachment> findByCitizenReportId(CitizenReportId citizenReportId) {
    return repository.findByCitizenReportIdOrderByTypeDesc(citizenReportId.value()).stream()
        .map(JpaSosAttachmentRepository::toDomain)
        .toList();
  }

  private static SosAttachment toDomain(SosAttachmentJpaEntity entity) {
    return new SosAttachment(
        new SosAttachmentId(entity.id),
        new CitizenReportId(entity.citizenReportId),
        entity.type,
        entity.contentType,
        entity.size,
        entity.storageUrl,
        entity.receivedAt);
  }
}
