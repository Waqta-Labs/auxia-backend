package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

import java.util.Objects;

/** First and last names of a user. */
public record PersonName(String firstName, String lastName) {

  public PersonName {
    firstName = requireText(firstName, "firstName");
    lastName = requireText(lastName, "lastName");
  }

  public String fullName() {
    return firstName + " " + lastName;
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }
}
