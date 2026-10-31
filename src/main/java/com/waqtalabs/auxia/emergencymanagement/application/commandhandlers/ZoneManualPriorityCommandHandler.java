package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.PrioritizeZoneManuallyCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RemoveZoneManualPriorityCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneManualPriorityRemoved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZonePrioritizedManually;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks a zone as a priority by hand, or takes the mark off (US-07). A manually prioritized zone
 * goes first in the ranking, ahead of the scores.
 */
@Service
public class ZoneManualPriorityCommandHandler {

  private final ZoneRepository zoneRepository;
  private final AuthorizationPort authorization;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public ZoneManualPriorityCommandHandler(
      ZoneRepository zoneRepository,
      AuthorizationPort authorization,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.zoneRepository = zoneRepository;
    this.authorization = authorization;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public Zone handle(PrioritizeZoneManuallyCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    Zone zone = load(command.zoneId());
    LocalDateTime now = LocalDateTime.now(clock);
    zone.prioritizeManually(command.authorityId(), command.reason(), now);
    Zone saved = zoneRepository.save(zone);
    eventPublisher.publish(
        new ZonePrioritizedManually(
            saved.id(), command.authorityId(), saved.manualPriority().orElseThrow().reason(), now));
    return saved;
  }

  /** A zone without a manual priority is returned as it is. */
  @Transactional
  public Zone handle(RemoveZoneManualPriorityCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    Zone zone = load(command.zoneId());
    if (!zone.isPrioritizedManually()) {
      return zone;
    }
    zone.removeManualPriority();
    Zone saved = zoneRepository.save(zone);
    eventPublisher.publish(
        new ZoneManualPriorityRemoved(saved.id(), command.authorityId(), LocalDateTime.now(clock)));
    return saved;
  }

  private Zone load(ZoneId zoneId) {
    return zoneRepository.findById(zoneId).orElseThrow(() -> new ZoneNotFoundException(zoneId));
  }
}
