package com.waqtalabs.auxia.shared.interfaces.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error of the API into an RFC 9457 problem detail.
 *
 * <p>The base class already handles the standard Spring MVC exceptions (malformed body, unknown
 * route, unsupported method…). Titles and details come from {@code messages*.properties} in the
 * language of the request.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final String UNEXPECTED_TITLE_CODE = "problemDetail.title.java.lang.Exception";
  private static final String UNEXPECTED_DETAIL_CODE = "problemDetail.java.lang.Exception";

  /** Adds the invalid fields, with their localized messages, to the validation error. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail body = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    body.setProperty("errors", ex.getFieldErrors().stream().map(InvalidField::from).toList());
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  /** Any exception without a specific handler: a 500 that does not expose internal details. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
    LOGGER.error("Unexpected error while processing {}", request.getDescription(false), ex);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    ProblemDetail body =
        ErrorResponse.builder(ex, status, "An unexpected error occurred.")
            .titleMessageCode(UNEXPECTED_TITLE_CODE)
            .detailMessageCode(UNEXPECTED_DETAIL_CODE)
            .build()
            .updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
  }

  /** One entry of the {@code errors} list of a validation problem. */
  record InvalidField(String field, String message) {

    static InvalidField from(FieldError error) {
      return new InvalidField(error.getField(), error.getDefaultMessage());
    }
  }
}
