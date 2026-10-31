package com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects;

import java.util.Objects;

/** Department, province and district of an emergency; a citizen finds their zone by them. */
public record PublicLocation(String department, String province, String district) {

  public PublicLocation {
    Objects.requireNonNull(department, "department");
    Objects.requireNonNull(province, "province");
    Objects.requireNonNull(district, "district");
  }

  /** For example "Lurigancho-Chosica, Lima, Lima": from the district to the department. */
  public String fullName() {
    return String.join(", ", district, province, department);
  }
}
