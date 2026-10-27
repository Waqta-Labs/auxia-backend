package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;

/** Department, province and district of an emergency. */
public record AdministrativeLocation(String department, String province, String district) {

  public AdministrativeLocation {
    department = requireText(department, "department");
    province = requireText(province, "province");
    district = requireText(district, "district");
  }

  /** For example "Chosica, Lima, Lima": from the district to the department. */
  public String fullName() {
    return String.join(", ", district, province, department);
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
