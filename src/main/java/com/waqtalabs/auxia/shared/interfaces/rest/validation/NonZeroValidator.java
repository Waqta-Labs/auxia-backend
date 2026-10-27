package com.waqtalabs.auxia.shared.interfaces.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NonZeroValidator implements ConstraintValidator<NonZero, Integer> {

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext context) {
    return value == null || value != 0;
  }
}
