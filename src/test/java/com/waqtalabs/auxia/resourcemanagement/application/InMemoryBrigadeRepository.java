package com.waqtalabs.auxia.resourcemanagement.application;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository. */
public class InMemoryBrigadeRepository implements BrigadeRepository {

  private final Map<BrigadeId, Brigade> brigades = new LinkedHashMap<>();

  @Override
  public Brigade save(Brigade brigade) {
    brigades.put(brigade.id(), brigade);
    return brigade;
  }

  @Override
  public Optional<Brigade> findById(BrigadeId id) {
    return Optional.ofNullable(brigades.get(id));
  }

  @Override
  public List<Brigade> findByOrganizationId(OrganizationId organizationId) {
    return brigades.values().stream()
        .filter(brigade -> brigade.belongsTo(organizationId))
        .sorted(Comparator.comparing(Brigade::name))
        .toList();
  }
}
