package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration.IdCard;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.BrigadeRegistrationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaBrigadeRegistrationRepository implements BrigadeRegistrationRepository {

  private final SpringDataBrigadeRegistrationRepository repository;

  JpaBrigadeRegistrationRepository(SpringDataBrigadeRegistrationRepository repository) {
    this.repository = repository;
  }

  @Override
  public BrigadeRegistration save(BrigadeRegistration registration) {
    var entity = new BrigadeRegistrationJpaEntity();
    entity.id = registration.id().value();
    entity.fullName = registration.fullName();
    entity.phone = registration.phone();
    entity.organizationName = registration.organizationName();
    entity.brigadeCode = registration.brigadeCode();
    entity.brigadeId = registration.brigadeId();
    entity.organizationId = registration.organizationId().value();
    entity.deviceSecretHash = registration.deviceSecret().value();
    entity.deviceSecretAlgorithm = registration.deviceSecret().algorithm();
    entity.idCardUrl = registration.idCard().map(IdCard::storageUrl).orElse(null);
    entity.idCardContentType = registration.idCard().map(IdCard::contentType).orElse(null);
    entity.status = registration.status();
    entity.rejectionReason = registration.rejectionReason().orElse(null);
    entity.submittedAt = registration.submittedAt();
    entity.decidedBy = registration.decidedBy().map(UserId::value).orElse(null);
    entity.decidedAt = registration.decidedAt().orElse(null);
    entity.userId = registration.userId().map(UserId::value).orElse(null);
    return toDomain(repository.save(entity));
  }

  @Override
  public Optional<BrigadeRegistration> findById(BrigadeRegistrationId id) {
    return repository.findById(id.value()).map(JpaBrigadeRegistrationRepository::toDomain);
  }

  @Override
  public List<BrigadeRegistration> findByOrganizationIdAndStatus(
      OrganizationId organizationId, RegistrationStatus status) {
    return repository
        .findByOrganizationIdAndStatusOrderBySubmittedAtAsc(organizationId.value(), status)
        .stream()
        .map(JpaBrigadeRegistrationRepository::toDomain)
        .toList();
  }

  private static BrigadeRegistration toDomain(BrigadeRegistrationJpaEntity entity) {
    return BrigadeRegistration.reconstitute(
        new BrigadeRegistrationId(entity.id),
        entity.fullName,
        entity.phone,
        entity.organizationName,
        entity.brigadeCode,
        entity.brigadeId,
        new OrganizationId(entity.organizationId),
        new PasswordHash(entity.deviceSecretHash, entity.deviceSecretAlgorithm),
        entity.idCardUrl == null ? null : new IdCard(entity.idCardUrl, entity.idCardContentType),
        entity.submittedAt,
        entity.status,
        entity.rejectionReason,
        userId(entity.decidedBy),
        entity.decidedAt,
        userId(entity.userId));
  }

  private static UserId userId(UUID value) {
    return value == null ? null : new UserId(value);
  }
}
