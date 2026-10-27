package com.waqtalabs.auxia.resourcemanagement.application.queryservices;

import com.waqtalabs.auxia.resourcemanagement.application.queries.GetBrigadeByIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetBrigadesByOrganizationIdQuery;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BrigadeQueryService {

  private final BrigadeRepository brigadeRepository;

  public BrigadeQueryService(BrigadeRepository brigadeRepository) {
    this.brigadeRepository = brigadeRepository;
  }

  public Brigade handle(GetBrigadeByIdQuery query) {
    return brigadeRepository
        .findById(query.brigadeId())
        .filter(brigade -> brigade.belongsTo(query.organizationId()))
        .orElseThrow(() -> new BrigadeNotFoundException(query.brigadeId()));
  }

  /** Brigades of the organization by name, filtered by status when the query has one. */
  public List<Brigade> handle(GetBrigadesByOrganizationIdQuery query) {
    return brigadeRepository.findByOrganizationId(query.organizationId()).stream()
        .filter(brigade -> query.status() == null || brigade.status() == query.status())
        .toList();
  }
}
