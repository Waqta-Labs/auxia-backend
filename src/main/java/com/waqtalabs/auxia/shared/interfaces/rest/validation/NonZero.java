package com.waqtalabs.auxia.shared.interfaces.rest.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** The number can be positive or negative, but not zero. {@code null} is valid. */
@Documented
@Constraint(validatedBy = NonZeroValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface NonZero {

  String message() default "must not be zero";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
