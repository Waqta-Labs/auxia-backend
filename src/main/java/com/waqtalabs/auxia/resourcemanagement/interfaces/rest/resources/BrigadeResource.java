package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record BrigadeResource(
    UUID id,
    UUID organizationId,
    String name,
    LocationResource baseLocation,
    BrigadeStatus status,
    @Schema(description = "A brigade needs a LEADER to be assigned to a plan") boolean hasLeader,
    @Schema(description = "Ordered by name") List<StaffMemberResource> members,
    @Schema(description = "Null while the brigade has no assignment in progress")
        BrigadeAssignmentResource currentAssignment) {}
