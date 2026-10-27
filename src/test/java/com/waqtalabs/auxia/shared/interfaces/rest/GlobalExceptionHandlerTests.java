package com.waqtalabs.auxia.shared.interfaces.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import com.waqtalabs.auxia.shared.infrastructure.i18n.LocalizationConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Only the sample controller: the controllers of the bounded contexts need their use cases. The
// security filter chain lets every request in, so each sample endpoint raises its own error.
@WebMvcTest(GlobalExceptionHandlerTests.SampleController.class)
@Import({
  GlobalExceptionHandlerTests.SampleController.class,
  LocalizationConfiguration.class,
  GlobalExceptionHandlerTests.OpenSecurityConfiguration.class
})
class GlobalExceptionHandlerTests {

  private static final String SAMPLES = "/test/samples";

  @Autowired private MockMvc mockMvc;

  @Test
  void invalidBodyReturnsBadRequestWithInvalidFields() throws Exception {
    mockMvc
        .perform(post(SAMPLES).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid request"))
        .andExpect(jsonPath("$.detail").value("One or more fields are invalid."))
        .andExpect(jsonPath("$.instance").value(SAMPLES))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
  }

  @Test
  void malformedJsonReturnsBadRequest() throws Exception {
    mockMvc
        .perform(post(SAMPLES).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Malformed request"));
  }

  @Test
  void unknownRouteReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/test/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not found"))
        .andExpect(jsonPath("$.detail").value("The requested resource does not exist."));
  }

  @Test
  void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
    mockMvc
        .perform(put(SAMPLES))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.title").value("Method not allowed"))
        .andExpect(jsonPath("$.detail").value("Method PUT is not supported by this endpoint."));
  }

  @Test
  void unexpectedExceptionReturnsInternalServerErrorWithoutInternalDetails() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/failure"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.title").value("Internal server error"))
        .andExpect(content().string(not(containsString("secret"))));
  }

  @Test
  void spanishRequestGetsLatinAmericanSpanishMessages() throws Exception {
    mockMvc
        .perform(
            post(SAMPLES)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es-PE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Solicitud no válida"))
        .andExpect(jsonPath("$.detail").value("Uno o más campos no son válidos."));
  }

  @Test
  void unsupportedLanguageFallsBackToEnglish() throws Exception {
    mockMvc
        .perform(get("/test/unknown").header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not found"));
  }

  @Test
  void domainNotFoundReturnsNotFoundWithTheMessageOfTheException() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/domain/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Not found"))
        .andExpect(jsonPath("$.detail").value("Sample 42 does not exist"));
  }

  @Test
  void domainConflictReturnsConflict() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/domain/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"));
  }

  @Test
  void domainRuleViolationReturnsUnprocessableContent() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/domain/rule-violation"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.title").value("Business rule violated"));
  }

  @Test
  void domainErrorTitleIsLocalized() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/domain/conflict").header(HttpHeaders.ACCEPT_LANGUAGE, "es-PE"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflicto"));
  }

  @Test
  void unavailableExternalServiceReturnsServiceUnavailableWithItsName() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/external-failure"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Service unavailable"))
        .andExpect(
            jsonPath("$.detail")
                .value("The AI Service is not available right now. Please try again later."))
        .andExpect(content().string(not(containsString("secret"))));
  }

  @Test
  void missingAuthenticationReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/security/unauthenticated"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Sign in to use this endpoint."));
  }

  @Test
  void missingRoleReturnsForbiddenInTheLanguageOfTheRequest() throws Exception {
    mockMvc
        .perform(get(SAMPLES + "/security/forbidden").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Acceso denegado"));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class OpenSecurityConfiguration {

    @Bean
    SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
          .build();
    }
  }

  /** Controller used only by these tests to trigger each kind of error. */
  @RestController
  @RequestMapping(SAMPLES)
  static class SampleController {

    record SampleResource(@NotBlank String name) {}

    @PostMapping
    SampleResource create(@Valid @RequestBody SampleResource resource) {
      return resource;
    }

    @GetMapping("/failure")
    SampleResource fail() {
      throw new IllegalStateException("secret connection detail");
    }

    @GetMapping("/external-failure")
    SampleResource failInExternalService() {
      throw new ExternalServiceUnavailableException(
          "AI Service", new IllegalStateException("secret connection detail"));
    }

    @GetMapping("/security/unauthenticated")
    SampleResource failWithoutAuthentication() {
      throw new AuthenticationCredentialsNotFoundException("No token");
    }

    @GetMapping("/security/forbidden")
    SampleResource failWithoutRole() {
      throw new AccessDeniedException("Access denied");
    }

    @GetMapping("/domain/{category}")
    SampleResource failInDomain(@PathVariable String category) {
      throw switch (category) {
        case "not-found" -> new SampleNotFoundException();
        case "conflict" -> new SampleConflictException();
        default -> new SampleRuleViolationException();
      };
    }
  }

  static class SampleNotFoundException extends NotFoundException {

    SampleNotFoundException() {
      super("Sample 42 does not exist", 42);
    }
  }

  static class SampleConflictException extends ConflictException {

    SampleConflictException() {
      super("Sample is closed");
    }
  }

  static class SampleRuleViolationException extends RuleViolationException {

    SampleRuleViolationException() {
      super("Sample breaks a rule");
    }
  }
}
