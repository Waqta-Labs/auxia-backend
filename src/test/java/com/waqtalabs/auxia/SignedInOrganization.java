package com.waqtalabs.auxia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * An organization registered through the API, with an ORGANIZATION_ADMIN, an AUTHORITY, a
 * FIELD_BRIGADE member and an AUDITOR who signed in. Their requests carry real tokens, so the JWT
 * filter and the checks of Identity Access run as in production; use it when a use case asks
 * Identity Access for the current role of the user.
 */
public record SignedInOrganization(
    UUID organizationId,
    UUID authorityId,
    String administratorToken,
    String authorityToken,
    String fieldBrigadeToken,
    String auditorToken) {

  private static final String PLATFORM_KEY = "test-platform-key-0123456789";
  private static final String PASSWORD = "Chosica-2026";

  public static SignedInOrganization register(MockMvc mockMvc) throws Exception {
    String administratorEmail = uniqueEmail();
    String body =
        mockMvc
            .perform(
                post("/api/v1/organizations")
                    .header("X-Platform-Key", PLATFORM_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "COER Lima",
                          "type": "GOVERNMENT",
                          "taxId": "20%d",
                          "administrator": {
                            "email": "%s",
                            "firstName": "Carlos",
                            "lastName": "Huamán",
                            "password": "%s"
                          }
                        }
                        """
                            .formatted(
                                ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L),
                                administratorEmail,
                                PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID organizationId = UUID.fromString(JsonPath.read(body, "$.organization.id"));
    String administratorToken = signIn(mockMvc, administratorEmail);
    String authorityEmail = uniqueEmail();
    UUID authorityId =
        registerUser(mockMvc, administratorToken, organizationId, authorityEmail, "AUTHORITY");
    String fieldBrigadeEmail = uniqueEmail();
    registerUser(mockMvc, administratorToken, organizationId, fieldBrigadeEmail, "FIELD_BRIGADE");
    String auditorEmail = uniqueEmail();
    registerUser(mockMvc, administratorToken, organizationId, auditorEmail, "AUDITOR");
    return new SignedInOrganization(
        organizationId,
        authorityId,
        administratorToken,
        signIn(mockMvc, authorityEmail),
        signIn(mockMvc, fieldBrigadeEmail),
        signIn(mockMvc, auditorEmail));
  }

  /** Another AUTHORITY of the organization, signed in: the next responsible of a shift. */
  public SignedInUser anotherAuthority(MockMvc mockMvc) throws Exception {
    String email = uniqueEmail();
    UUID id = registerUser(mockMvc, administratorToken, organizationId, email, "AUTHORITY");
    return new SignedInUser(id, bearer(signIn(mockMvc, email)));
  }

  /** A user who signed in, with the processor that adds its token to a request. */
  public record SignedInUser(UUID id, RequestPostProcessor token) {}

  public RequestPostProcessor administrator() {
    return bearer(administratorToken);
  }

  public RequestPostProcessor authority() {
    return bearer(authorityToken);
  }

  public RequestPostProcessor fieldBrigade() {
    return bearer(fieldBrigadeToken);
  }

  public RequestPostProcessor auditor() {
    return bearer(auditorToken);
  }

  private static RequestPostProcessor bearer(String token) {
    return request -> {
      request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
      return request;
    };
  }

  private static UUID registerUser(
      MockMvc mockMvc, String token, UUID organizationId, String email, String role)
      throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/organizations/" + organizationId + "/users")
                    .with(bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "%s",
                          "firstName": "Rosa",
                          "lastName": "Quispe",
                          "role": "%s",
                          "password": "%s"
                        }
                        """
                            .formatted(email, role, PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  private static String signIn(MockMvc mockMvc, String email) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/sign-in")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.token");
  }

  private static String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@auxia.pe";
  }
}
