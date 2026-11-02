package com.waqtalabs.auxia.shared.interfaces.rest;

import com.waqtalabs.auxia.shared.application.exceptions.ActionNotAllowedException;
import com.waqtalabs.auxia.shared.application.exceptions.AuthenticationFailedException;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;
import com.waqtalabs.auxia.shared.domain.exceptions.DomainException;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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

  /**
   * An error of the domain: 404, 409 or 422 depending on its category. The title belongs to the
   * category and the detail to the concrete exception, filled with its arguments; its properties
   * are added as members of the problem detail.
   */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Object> handleDomainException(DomainException ex, WebRequest request) {
    Class<? extends DomainException> category =
        switch (ex) {
          case NotFoundException ignored -> NotFoundException.class;
          case ConflictException ignored -> ConflictException.class;
          case RuleViolationException ignored -> RuleViolationException.class;
        };
    HttpStatus status =
        switch (ex) {
          case NotFoundException ignored -> HttpStatus.NOT_FOUND;
          case ConflictException ignored -> HttpStatus.CONFLICT;
          case RuleViolationException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    ProblemDetail body =
        ErrorResponse.builder(ex, status, ex.getMessage())
            .titleMessageCode(ErrorResponse.getDefaultTitleMessageCode(category))
            .detailMessageCode(ErrorResponse.getDefaultDetailMessageCode(ex.getClass(), null))
            .detailMessageArguments(ex.arguments())
            .build()
            .updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    ex.properties().forEach(body::setProperty);
    return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
  }

  /**
   * Wrong credentials: 401 with one generic message, whatever failed (US-21). The reason stays in
   * the log only.
   */
  @ExceptionHandler(AuthenticationFailedException.class)
  public ResponseEntity<Object> handleAuthenticationFailed(
      AuthenticationFailedException ex, WebRequest request) {
    LOGGER.info("Authentication failed: {}", ex.getMessage());
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    ProblemDetail body =
        ErrorResponse.builder(ex, status, "Invalid credentials.")
            .build()
            .updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
  }

  /** A request to a protected endpoint without a valid token: 401. */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Object> handleMissingAuthentication(
      AuthenticationException ex, WebRequest request) {
    return securityProblem(ex, HttpStatus.UNAUTHORIZED, "authenticationRequired", request);
  }

  /** The user is signed in but their role cannot run the action (US-22): 403. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    return securityProblem(ex, HttpStatus.FORBIDDEN, "accessDenied", request);
  }

  /** A use case found that the user cannot run it now: 403, like a missing role. */
  @ExceptionHandler(ActionNotAllowedException.class)
  public ResponseEntity<Object> handleActionNotAllowed(
      ActionNotAllowedException ex, WebRequest request) {
    LOGGER.info("Action not allowed: {}", ex.getMessage());
    return securityProblem(ex, HttpStatus.FORBIDDEN, "accessDenied", request);
  }

  /** An external service, such as the AI Service, did not answer: 503, so the client retries. */
  @ExceptionHandler(ExternalServiceUnavailableException.class)
  public ResponseEntity<Object> handleExternalServiceUnavailable(
      ExternalServiceUnavailableException ex, WebRequest request) {
    HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
    ProblemDetail body =
        ErrorResponse.builder(ex, status, ex.getMessage())
            .detailMessageArguments(ex.serviceName())
            .build()
            .updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
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

  private ResponseEntity<Object> securityProblem(
      Exception ex, HttpStatus status, String key, WebRequest request) {
    ProblemDetail body =
        ErrorResponse.builder(ex, status, ex.getMessage())
            .titleMessageCode("problemDetail.title.security." + key)
            .detailMessageCode("problemDetail.security." + key)
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
