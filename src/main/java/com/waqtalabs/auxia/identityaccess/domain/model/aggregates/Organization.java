package com.waqtalabs.auxia.identityaccess.domain.model.aggregates;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import java.util.Objects;

/**
 * Public entity, NGO or private company that uses AuxIA. While it is suspended, none of its users
 * can sign in or pass a role check, even if their accounts are active.
 */
public final class Organization {

  private final OrganizationId id;
  private final String name;
  private final OrganizationType type;
  private final TaxId taxId;
  private OrganizationStatus status;

  private Organization(
      OrganizationId id,
      String name,
      OrganizationType type,
      TaxId taxId,
      OrganizationStatus status) {
    this.id = Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.name = name.strip();
    this.type = Objects.requireNonNull(type, "type");
    this.taxId = Objects.requireNonNull(taxId, "taxId");
    this.status = Objects.requireNonNull(status, "status");
  }

  /** Registers an active organization. */
  public static Organization register(String name, OrganizationType type, TaxId taxId) {
    return new Organization(
        OrganizationId.generate(), name, type, taxId, OrganizationStatus.ACTIVE);
  }

  /** Rebuilds an organization that is already stored. */
  public static Organization reconstitute(
      OrganizationId id,
      String name,
      OrganizationType type,
      TaxId taxId,
      OrganizationStatus status) {
    return new Organization(id, name, type, taxId, status);
  }

  /** Suspends the organization and, with it, the access of all its users. */
  public void suspend() {
    status = OrganizationStatus.SUSPENDED;
  }

  public void reactivate() {
    status = OrganizationStatus.ACTIVE;
  }

  public boolean isActive() {
    return status == OrganizationStatus.ACTIVE;
  }

  public OrganizationId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public OrganizationType type() {
    return type;
  }

  public TaxId taxId() {
    return taxId;
  }

  public OrganizationStatus status() {
    return status;
  }
}
