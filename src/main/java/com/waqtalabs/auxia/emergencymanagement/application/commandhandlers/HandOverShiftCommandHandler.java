package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.HandOverShiftCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorityDirectory;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ShiftHandedOver;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IneligibleIncomingAuthorityException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ShiftHandover;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ShiftHandoverRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers the shift change of an authority (US-23). The zones of active emergencies it follows
 * pass to the incoming authority, which must be an active authority of the same organization; the
 * zones of closed emergencies keep their responsible. ShiftHandedOver adds the change to the
 * history of each zone.
 */
@Service
public class HandOverShiftCommandHandler {

  private final ZoneRepository zoneRepository;
  private final EmergencyRepository emergencyRepository;
  private final ShiftHandoverRepository shiftHandoverRepository;
  private final AuthorizationPort authorization;
  private final AuthorityDirectory authorities;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public HandOverShiftCommandHandler(
      ZoneRepository zoneRepository,
      EmergencyRepository emergencyRepository,
      ShiftHandoverRepository shiftHandoverRepository,
      AuthorizationPort authorization,
      AuthorityDirectory authorities,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.zoneRepository = zoneRepository;
    this.emergencyRepository = emergencyRepository;
    this.shiftHandoverRepository = shiftHandoverRepository;
    this.authorization = authorization;
    this.authorities = authorities;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public ShiftHandover handle(HandOverShiftCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.outgoingAuthorityId());
    if (!authorization.isActiveAuthority(command.incomingAuthorityId())
        || !authorities
            .organizationOf(command.incomingAuthorityId())
            .equals(Optional.of(command.organizationId()))) {
      throw new IneligibleIncomingAuthorityException(command.incomingAuthorityId());
    }
    List<Zone> zones =
        zoneRepository.findByResponsibleAuthority(command.outgoingAuthorityId()).stream()
            .filter(this::inActiveEmergency)
            .toList();
    LocalDateTime now = LocalDateTime.now(clock);
    ShiftHandover handover =
        ShiftHandover.record(
            command.organizationId(),
            command.outgoingAuthorityId(),
            command.incomingAuthorityId(),
            zones.stream().map(Zone::id).toList(),
            command.notes(),
            now);
    for (Zone zone : zones) {
      zone.assignResponsible(command.incomingAuthorityId());
      zoneRepository.save(zone);
    }
    ShiftHandover saved = shiftHandoverRepository.save(handover);
    eventPublisher.publish(
        new ShiftHandedOver(
            saved.id(),
            saved.outgoing(),
            saved.incoming(),
            saved.zoneIds(),
            saved.notes().orElse(null),
            now));
    return saved;
  }

  private boolean inActiveEmergency(Zone zone) {
    return emergencyRepository
        .findById(zone.emergencyId())
        .map(Emergency::isActive)
        .orElse(false);
  }
}
