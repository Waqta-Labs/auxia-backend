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

import com.waqtalabs.auxia.shared.infrastructure.i18n.LocalizationConfiguration;
import com.waqtalabs.auxia.shared.infrastructure.security.WebSecurityConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({
  GlobalExceptionHandlerTests.SampleController.class,
  LocalizationConfiguration.class,
  WebSecurityConfiguration.class
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
  }
}
