package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import java.util.Objects;
import java.util.Optional;

/** Member of a brigade. Entity inside the {@link Brigade} aggregate. */
public final class StaffMember {

  private final StaffMemberId id;
  private final String fullName;
  private final StaffRole role;
  private final String phone;
  private final UserId userId;

  private StaffMember(
      StaffMemberId id, String fullName, StaffRole role, String phone, UserId userId) {
    this.id = Objects.requireNonNull(id, "id");
    Objects.requireNonNull(fullName, "fullName");
    if (fullName.isBlank()) {
      throw new IllegalArgumentException("fullName must not be blank");
    }
    this.fullName = fullName.strip();
    this.role = Objects.requireNonNull(role, "role");
    this.phone = phone == null || phone.isBlank() ? null : phone.strip();
    this.userId = userId;
  }

  /**
   * Registers a member, to add it to a brigade.
   *
   * @param phone optional
   */
  public static StaffMember register(String fullName, StaffRole role, String phone) {
    return register(fullName, role, phone, null);
  }

  /**
   * Registers a member that signs in to the mobile app with an account, which then sees the
   * missions of the brigade (A_07).
   *
   * @param userId {@code null} for a member without an account
   */
  public static StaffMember register(
      String fullName, StaffRole role, String phone, UserId userId) {
    return new StaffMember(StaffMemberId.generate(), fullName, role, phone, userId);
  }

  /** Rebuilds a member that is already stored. */
  public static StaffMember reconstitute(
      StaffMemberId id, String fullName, StaffRole role, String phone, UserId userId) {
    return new StaffMember(id, fullName, role, phone, userId);
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

  /** The account the member signs in with; empty when it has none. */
  public Optional<UserId> userId() {
    return Optional.ofNullable(userId);
  }
}
