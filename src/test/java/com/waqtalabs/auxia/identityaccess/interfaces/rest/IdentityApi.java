package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Calls the identity endpoints the way a client does: the platform key registers organizations,
 * and every other request carries the bearer token returned by the sign-in, so the JWT filter
 * runs as in production.
 */
final class IdentityApi {

  /** The PLATFORM_ADMIN_KEY that Surefire sets for the tests. */
  static final String PLATFORM_KEY = "test-platform-key-0123456789";

  static final String PASSWORD = "Chosica-2026";

  private final MockMvc mockMvc;

  IdentityApi(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  /** An organization registered by the platform operator, with its first administrator. */
  record RegisteredOrganization(String id, String administratorId, String administratorEmail) {}

  RegisteredOrganization registerOrganization() throws Exception {
    String email = uniqueEmail();
    String body =
        registerOrganization(organizationRequest(uniqueTaxId(), email), PLATFORM_KEY)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new RegisteredOrganization(
        JsonPath.read(body, "$.organization.id"), JsonPath.read(body, "$.administrator.id"), email);
  }

  ResultActions registerOrganization(String body, String platformKey) throws Exception {
    var request = post("/api/v1/organizations").contentType(MediaType.APPLICATION_JSON);
    if (platformKey != null) {
      request.header(PlatformKeyGuard.HEADER, platformKey);
    }
    return mockMvc.perform(request.content(body));
  }

  ResultActions changeOrganizationStatus(String organizationId, String status) throws Exception {
    return mockMvc.perform(
        patch("/api/v1/organizations/" + organizationId + "/status")
            .header(PlatformKeyGuard.HEADER, PLATFORM_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"" + status + "\"}"));
  }

  ResultActions signIn(String email, String password) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
  }

  /** The token of a successful sign-in with {@link #PASSWORD}. */
  String tokenOf(String email) throws Exception {
    String body =
        signIn(email, PASSWORD)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.token");
  }

  ResultActions registerUser(String token, String organizationId, String email, String role)
      throws Exception {
    return mockMvc.perform(
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
                    .formatted(email, role, PASSWORD)));
  }

  /** The id of a user registered by the administrator of the organization. */
  String registeredUserId(String token, String organizationId, String email, String role)
      throws Exception {
    String body =
        registerUser(token, organizationId, email, role)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  ResultActions changeUser(String token, String userId, String field, String value)
      throws Exception {
    return mockMvc.perform(
        patch("/api/v1/users/" + userId + "/" + field)
            .with(bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"%s\": \"%s\"}".formatted(field, value)));
  }

  static RequestPostProcessor bearer(String token) {
    return request -> {
      request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
      return request;
    };
  }

  static String organizationRequest(String taxId, String administratorEmail) {
    return """
        {
          "name": "COER Lima",
          "type": "GOVERNMENT",
          "taxId": "%s",
          "administrator": {
            "email": "%s",
            "firstName": "Carlos",
            "lastName": "Huamán",
            "password": "%s"
          }
        }
        """
        .formatted(taxId, administratorEmail, PASSWORD);
  }

  static String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@auxia.pe";
  }

  static String uniqueTaxId() {
    return "20" + ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
  }
}
