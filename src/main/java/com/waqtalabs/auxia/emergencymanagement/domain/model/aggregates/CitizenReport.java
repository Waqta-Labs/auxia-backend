package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * SOS that a citizen sends from the mobile app, without signing in.
 *
 * <p>The phone generates its identifier, so the same SOS sent again is recognized. When the SOS
 * can be placed in an active emergency it is also registered as a field report of source {@code
 * CITIZEN}, which the authority reviews like any other; the citizen follows its progress through
 * that report. A later SOS can add detail to an earlier one.
 */
public final class CitizenReport {

  private final CitizenReportId id;
  private final SosCategory category;
  private final String text;
  private final LocalDateTime reportedAt;
  private final LocalDateTime receivedAt;
  private final Geolocation location;
  private final VulnerablePeople people;
  private final Set<AccessCondition> accessConditions;
  private final CitizenReportId relatedReportId;
  private final FieldReportId fieldReportId;

  private CitizenReport(
      CitizenReportId id,
      SosCategory category,
      String text,
      LocalDateTime reportedAt,
      LocalDateTime receivedAt,
      Geolocation location,
      VulnerablePeople people,
      Collection<AccessCondition> accessConditions,
      CitizenReportId relatedReportId,
      FieldReportId fieldReportId) {
    this.id = Objects.requireNonNull(id, "id");
    this.category = Objects.requireNonNull(category, "category");
    this.text = requireText(text);
    this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt");
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
    this.location = location;
    this.people = Objects.requireNonNull(people, "people");
    this.accessConditions = inDeclarationOrder(accessConditions);
    if (id.equals(relatedReportId)) {
      throw new IllegalArgumentException("An SOS cannot add detail to itself");
    }
    this.relatedReportId = relatedReportId;
    this.fieldReportId = fieldReportId;
  }

  /**
   * Receives an SOS.
   *
   * @param reportedAt when the citizen sent it, by the clock of the phone
   * @param location {@code null} when the phone had no GPS
   * @param relatedReportId earlier SOS this one adds detail to, or {@code null}
   * @param fieldReportId field report the SOS became, or {@code null} when no active emergency
   *     could be found for it
   */
  public static CitizenReport receive(
      CitizenReportId id,
      SosCategory category,
      String text,
      LocalDateTime reportedAt,
      LocalDateTime receivedAt,
      Geolocation location,
      VulnerablePeople people,
      Collection<AccessCondition> accessConditions,
      CitizenReportId relatedReportId,
      FieldReportId fieldReportId) {
    return new CitizenReport(
        id,
        category,
        text,
        reportedAt,
        receivedAt,
        location,
        people,
        accessConditions,
        relatedReportId,
        fieldReportId);
  }

  /** Rebuilds an SOS that is already stored. */
  public static CitizenReport reconstitute(
      CitizenReportId id,
      SosCategory category,
      String text,
      LocalDateTime reportedAt,
      LocalDateTime receivedAt,
      Geolocation location,
      VulnerablePeople people,
      Collection<AccessCondition> accessConditions,
      CitizenReportId relatedReportId,
      FieldReportId fieldReportId) {
    return receive(
        id,
        category,
        text,
        reportedAt,
        receivedAt,
        location,
        people,
        accessConditions,
        relatedReportId,
        fieldReportId);
  }

  /**
   * When the situation was reported: the time of the phone, unless the phone clock is ahead of
   * the server, in which case the time the SOS arrived. An SOS queued offline keeps its time.
   */
  public static LocalDateTime reportedTime(LocalDateTime reportedAt, LocalDateTime receivedAt) {
    return reportedAt.isAfter(receivedAt) ? receivedAt : reportedAt;
  }

  public CitizenReportId id() {
    return id;
  }

  public SosCategory category() {
    return category;
  }

  public String text() {
    return text;
  }

  public LocalDateTime reportedAt() {
    return reportedAt;
  }

  public LocalDateTime receivedAt() {
    return receivedAt;
  }

  public Optional<Geolocation> location() {
    return Optional.ofNullable(location);
  }

  public VulnerablePeople people() {
    return people;
  }

  public Set<AccessCondition> accessConditions() {
    return accessConditions;
  }

  public Optional<CitizenReportId> relatedReportId() {
    return Optional.ofNullable(relatedReportId);
  }

  /** The field report the SOS became; empty when no active emergency could be found for it. */
  public Optional<FieldReportId> fieldReportId() {
    return Optional.ofNullable(fieldReportId);
  }

  private static Set<AccessCondition> inDeclarationOrder(Collection<AccessCondition> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return Set.of();
    }
    return Collections.unmodifiableSet(EnumSet.copyOf(conditions));
  }

  private static String requireText(String value) {
    Objects.requireNonNull(value, "text");
    if (value.isBlank()) {
      throw new IllegalArgumentException("text must not be blank");
    }
    return value.strip();
  }
}
