package com.waqtalabs.auxia.citizentransparency.domain.model.readmodels;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicLocation;
import java.time.LocalDate;
import java.util.Objects;

/** Public data of an emergency: only what a citizen needs to recognize it. */
public final class EmergencyPublicView {

  private final EmergencyId emergencyId;
  private final String name;
  private final String type;
  private final PublicLocation location;
  private final LocalDate startDate;

  private EmergencyPublicView(
      EmergencyId emergencyId,
      String name,
      String type,
      PublicLocation location,
      LocalDate startDate) {
    this.emergencyId = Objects.requireNonNull(emergencyId, "emergencyId");
    this.name = Objects.requireNonNull(name, "name");
    this.type = Objects.requireNonNull(type, "type");
    this.location = Objects.requireNonNull(location, "location");
    this.startDate = Objects.requireNonNull(startDate, "startDate");
  }

  /** The view of an emergency just registered; also rebuilds a stored one. */
  public static EmergencyPublicView project(
      EmergencyId emergencyId,
      String name,
      String type,
      PublicLocation location,
      LocalDate startDate) {
    return new EmergencyPublicView(emergencyId, name, type, location, startDate);
  }

  public EmergencyId emergencyId() {
    return emergencyId;
  }

  public String name() {
    return name;
  }

  public String type() {
    return type;
  }

  public PublicLocation location() {
    return location;
  }

  public LocalDate startDate() {
    return startDate;
  }
}
