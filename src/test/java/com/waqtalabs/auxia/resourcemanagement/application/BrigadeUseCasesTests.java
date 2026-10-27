package com.waqtalabs.auxia.resourcemanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AddStaffMemberCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ChangeBrigadeAvailabilityCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.CreateBrigadeCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.RemoveStaffMemberCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.AddStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ChangeBrigadeAvailabilityCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.CreateBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.RemoveStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetBrigadeByIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetBrigadesByOrganizationIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.BrigadeQueryService;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.DuplicateBrigadeException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BrigadeUseCasesTests {

  private static final Location CHOSICA = new Location(-11.9436, -76.7078);

  private final InMemoryBrigadeRepository brigades = new InMemoryBrigadeRepository();
  private final CreateBrigadeCommandHandler create = new CreateBrigadeCommandHandler(brigades);
  private final AddStaffMemberCommandHandler addMember =
      new AddStaffMemberCommandHandler(brigades);
  private final RemoveStaffMemberCommandHandler removeMember =
      new RemoveStaffMemberCommandHandler(brigades);
  private final ChangeBrigadeAvailabilityCommandHandler changeAvailability =
      new ChangeBrigadeAvailabilityCommandHandler(brigades);
  private final BrigadeQueryService queries = new BrigadeQueryService(brigades);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());

  @Test
  void brigadeNameIsUniqueInsideItsOrganizationOnly() {
    create.handle(new CreateBrigadeCommand(organization, "Brigada Chosica 1", CHOSICA));

    assertThatThrownBy(
            () ->
                create.handle(
                    new CreateBrigadeCommand(organization, "brigada chosica 1", CHOSICA)))
        .isInstanceOf(DuplicateBrigadeException.class);
    var otherOrganization = new OrganizationId(UUID.randomUUID());
    assertThat(
            create.handle(
                new CreateBrigadeCommand(otherOrganization, "Brigada Chosica 1", CHOSICA)))
        .isNotNull();
  }

  @Test
  void membersAreAddedAndRemoved() {
    Brigade brigade = create();

    Brigade withMember =
        addMember.handle(
            new AddStaffMemberCommand(
                brigade.id(), organization, "Rosa Quispe", StaffRole.LEADER, null));
    StaffMember leader = withMember.members().getFirst();
    assertThat(withMember.hasLeader()).isTrue();

    Brigade withoutMember =
        removeMember.handle(
            new RemoveStaffMemberCommand(brigade.id(), organization, leader.id()));
    assertThat(withoutMember.members()).isEmpty();
  }

  @Test
  void brigadesAreListedByStatus() {
    Brigade offDuty = create();
    Brigade available =
        create.handle(new CreateBrigadeCommand(organization, "Brigada Pedregal", CHOSICA));
    changeAvailability.handle(
        new ChangeBrigadeAvailabilityCommand(offDuty.id(), organization, BrigadeStatus.OFF_DUTY));

    assertThat(
            queries.handle(
                new GetBrigadesByOrganizationIdQuery(organization, BrigadeStatus.AVAILABLE)))
        .extracting(Brigade::id)
        .containsExactly(available.id());
    assertThat(queries.handle(new GetBrigadesByOrganizationIdQuery(organization, null)))
        .hasSize(2);
  }

  @Test
  void unknownBrigadeIsNotFound() {
    var unknown = BrigadeId.generate();

    assertThatThrownBy(() -> queries.handle(new GetBrigadeByIdQuery(unknown, organization)))
        .isInstanceOf(BrigadeNotFoundException.class);
    assertThatThrownBy(
            () ->
                addMember.handle(
                    new AddStaffMemberCommand(
                        unknown, organization, "Rosa Quispe", StaffRole.LEADER, null)))
        .isInstanceOf(BrigadeNotFoundException.class);
  }

  @Test
  void brigadeOfAnotherOrganizationIsNotFound() {
    Brigade brigade = create();
    var otherOrganization = new OrganizationId(UUID.randomUUID());

    assertThatThrownBy(
            () -> queries.handle(new GetBrigadeByIdQuery(brigade.id(), otherOrganization)))
        .isInstanceOf(BrigadeNotFoundException.class);
    assertThatThrownBy(
            () ->
                changeAvailability.handle(
                    new ChangeBrigadeAvailabilityCommand(
                        brigade.id(), otherOrganization, BrigadeStatus.OFF_DUTY)))
        .isInstanceOf(BrigadeNotFoundException.class);
    assertThat(brigades.findById(brigade.id()).orElseThrow().status())
        .isEqualTo(BrigadeStatus.AVAILABLE);
  }

  private Brigade create() {
    return create.handle(new CreateBrigadeCommand(organization, "Brigada Chosica 1", CHOSICA));
  }
}
