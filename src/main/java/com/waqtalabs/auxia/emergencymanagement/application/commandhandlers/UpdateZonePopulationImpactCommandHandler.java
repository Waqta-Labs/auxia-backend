package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.UpdateZonePopulationImpactCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZonePopulationImpactUpdated;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates the population impact of a zone, for example to add its population, which the urgency
 * score needs. It publishes ZonePopulationImpactUpdated, so the score is recalculated.
 */
@Service
public class UpdateZonePopulationImpactCommandHandler {

  private final ZoneRepository zoneRepository;
  private final DomainEventPublisher eventPublisher;

  public UpdateZonePopulationImpactCommandHandler(
      ZoneRepository zoneRepository, DomainEventPublisher eventPublisher) {
    this.zoneRepository = zoneRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Zone handle(UpdateZonePopulationImpactCommand command) {
    Zone zone =
        zoneRepository
            .findById(command.zoneId())
            .orElseThrow(() -> new ZoneNotFoundException(command.zoneId()));
    PopulationImpact current = zone.populationImpact();
    zone.updatePopulationImpact(
        new PopulationImpact(
            orCurrent(command.population(), current.population()),
            orCurrent(command.familiesAffected(), current.familiesAffected()),
            orCurrent(command.peopleAffected(), current.peopleAffected()),
            orCurrent(command.children(), current.children()),
            orCurrent(command.elderlyPeople(), current.elderlyPeople()),
            orCurrent(command.disabledPeople(), current.disabledPeople())));
    Zone saved = zoneRepository.save(zone);
    eventPublisher.publish(new ZonePopulationImpactUpdated(saved.id()));
    return saved;
  }

  private static Integer orCurrent(Integer value, Integer current) {
    return value != null ? value : current;
  }
}
