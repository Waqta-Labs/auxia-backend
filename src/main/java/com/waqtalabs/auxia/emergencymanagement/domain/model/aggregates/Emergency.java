package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyAlreadyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Groups the zones affected by the same disaster and controls whether it is still active. */
public final class Emergency {

  private final EmergencyId id;
  private final String name;
  private final EmergencyType type;
  private final String description;
  private final AdministrativeLocation location;
  private final LocalDate startDate;
  private EmergencyStatus status;

  private Emergency(
      EmergencyId id,
      String name,
      EmergencyType type,
      String description,
      AdministrativeLocation location,
      LocalDate startDate,
      EmergencyStatus status) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireText(name, "name");
    this.type = Objects.requireNonNull(type, "type");
    this.description = description == null || description.isBlank() ? null : description.strip();
    this.location = Objects.requireNonNull(location, "location");
    this.startDate = Objects.requireNonNull(startDate, "startDate");
    this.status = Objects.requireNonNull(status, "status");
  }

  /** Registers a new emergency in {@code ACTIVE} status. The description is optional. */
  public static Emergency register(
      String name,
      EmergencyType type,
      String description,
      AdministrativeLocation location,
      LocalDate startDate) {
    return new Emergency(
        EmergencyId.generate(),
        name,
        type,
        description,
        location,
        startDate,
        EmergencyStatus.ACTIVE);
  }

  /** Rebuilds an emergency that is already stored, without registering it again. */
  public static Emergency reconstitute(
      EmergencyId id,
      String name,
      EmergencyType type,
      String description,
      AdministrativeLocation location,
      LocalDate startDate,
      EmergencyStatus status) {
    return new Emergency(id, name, type, description, location, startDate, status);
  }

  /** Closes the emergency. From then on it does not accept new zones. */
  public void close() {
    if (!isActive()) {
      throw new EmergencyAlreadyClosedException(id);
    }
    status = EmergencyStatus.CLOSED;
  }

  public boolean isActive() {
    return status == EmergencyStatus.ACTIVE;
  }

  /**
   * Registers a zone affected by this emergency. The geolocation is optional.
   *
   * @throws EmergencyClosedException if the emergency is closed
   */
  public Zone registerZone(
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel) {
    if (!isActive()) {
      throw new EmergencyClosedException(id);
    }
    return Zone.register(id, name, geolocation, populationImpact, damageLevel);
  }

  public EmergencyId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public EmergencyType type() {
    return type;
  }

  public Optional<String> description() {
    return Optional.ofNullable(description);
  }

  public AdministrativeLocation location() {
    return location;
  }

  public LocalDate startDate() {
    return startDate;
  }

  public EmergencyStatus status() {
    return status;
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
