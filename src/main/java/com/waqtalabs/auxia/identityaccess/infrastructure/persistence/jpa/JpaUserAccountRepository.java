package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserAccountRepository implements UserAccountRepository {

  private final SpringDataUserAccountRepository repository;

  JpaUserAccountRepository(SpringDataUserAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserAccount save(UserAccount userAccount) {
    UserAccountJpaEntity entity =
        repository.findById(userAccount.id().value()).orElseGet(UserAccountJpaEntity::new);
    UserAccountJpaMapper.copy(userAccount, entity);
    return UserAccountJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<UserAccount> findById(UserId id) {
    return repository.findById(id.value()).map(UserAccountJpaMapper::toDomain);
  }

  @Override
  public Optional<UserAccount> findByEmail(EmailAddress email) {
    return repository.findByEmail(email.value()).map(UserAccountJpaMapper::toDomain);
  }

  @Override
  public boolean existsByEmail(EmailAddress email) {
    return repository.existsByEmail(email.value());
  }

  @Override
  public List<UserAccount> findByOrganizationId(OrganizationId organizationId) {
    return repository.findByOrganizationIdOrderByEmailAsc(organizationId.value()).stream()
        .map(UserAccountJpaMapper::toDomain)
        .toList();
  }
}
