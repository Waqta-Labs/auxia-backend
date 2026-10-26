package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import java.util.Objects;
import java.util.Optional;

/** Impact and attention status of an area affected by an emergency. */
public final class Zone {

  private final ZoneId id;
  private final EmergencyId emergencyId;
  private final String name;
  private final Geolocation geolocation;
  private final PopulationImpact populationImpact;
  private final DamageLevel damageLevel;
  private final ZoneStatus status;

  private Zone(
      ZoneId id,
      EmergencyId emergencyId,
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel,
      ZoneStatus status) {
    this.id = Objects.requireNonNull(id, "id");
    this.emergencyId = Objects.requireNonNull(emergencyId, "emergencyId");
    this.name = requireText(name);
    this.geolocation = geolocation;
    this.populationImpact = Objects.requireNonNull(populationImpact, "populationImpact");
    this.damageLevel = Objects.requireNonNull(damageLevel, "damageLevel");
    this.status = Objects.requireNonNull(status, "status");
  }

  /**
   * Registers a zone in {@code REGISTERED} status. The geolocation is optional. Use {@link
   * Emergency#registerZone}, which first checks that the emergency is still active.
   */
  static Zone register(
      EmergencyId emergencyId,
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel) {
    return new Zone(
        ZoneId.generate(),
        emergencyId,
        name,
        geolocation,
        populationImpact,
        damageLevel,
        ZoneStatus.REGISTERED);
  }

  /** Rebuilds a zone that is already stored, without registering it again. */
  public static Zone reconstitute(
      ZoneId id,
      EmergencyId emergencyId,
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel,
      ZoneStatus status) {
    return new Zone(id, emergencyId, name, geolocation, populationImpact, damageLevel, status);
  }

  public boolean belongsTo(EmergencyId emergencyId) {
    return this.emergencyId.equals(emergencyId);
  }

  public ZoneId id() {
    return id;
  }

  public EmergencyId emergencyId() {
    return emergencyId;
  }

  public String name() {
    return name;
  }

  public Optional<Geolocation> geolocation() {
    return Optional.ofNullable(geolocation);
  }

  public PopulationImpact populationImpact() {
    return populationImpact;
  }

  public DamageLevel damageLevel() {
    return damageLevel;
  }

  public ZoneStatus status() {
    return status;
  }

  private static String requireText(String value) {
    Objects.requireNonNull(value, "name");
    if (value.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return value.strip();
  }
}
