package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import java.util.Objects;
import java.util.Optional;

/** Member of a brigade. Entity inside the {@link Brigade} aggregate. */
public final class StaffMember {

  private final StaffMemberId id;
  private final String fullName;
  private final StaffRole role;
  private final String phone;

  private StaffMember(StaffMemberId id, String fullName, StaffRole role, String phone) {
    this.id = Objects.requireNonNull(id, "id");
    Objects.requireNonNull(fullName, "fullName");
    if (fullName.isBlank()) {
      throw new IllegalArgumentException("fullName must not be blank");
    }
    this.fullName = fullName.strip();
    this.role = Objects.requireNonNull(role, "role");
    this.phone = phone == null || phone.isBlank() ? null : phone.strip();
  }

  /**
   * Registers a member, to add it to a brigade.
   *
   * @param phone optional
   */
  public static StaffMember register(String fullName, StaffRole role, String phone) {
    return new StaffMember(StaffMemberId.generate(), fullName, role, phone);
  }

  /** Rebuilds a member that is already stored. */
  public static StaffMember reconstitute(
      StaffMemberId id, String fullName, StaffRole role, String phone) {
    return new StaffMember(id, fullName, role, phone);
  }

  public boolean isLeader() {
    return role == StaffRole.LEADER;
  }

  public StaffMemberId id() {
    return id;
  }

  public String fullName() {
    return fullName;
  }

  public StaffRole role() {
    return role;
  }

  public Optional<String> phone() {
    return Optional.ofNullable(phone);
  }
}
