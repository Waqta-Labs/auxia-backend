package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.PASSWORD;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.bearer;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.uniqueEmail;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.RegisteredOrganization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthenticationControllerTests {

  static final String ME = "/api/v1/users/me";

  @Autowired private MockMvc mockMvc;

  private IdentityApi api;
  private RegisteredOrganization organization;
  private String administratorToken;

  @BeforeEach
  void registerOrganization() throws Exception {
    api = new IdentityApi(mockMvc);
    organization = api.registerOrganization();
    administratorToken = api.tokenOf(organization.administratorEmail());
  }

  @Test
  void signInReturnsABearerTokenThatIdentifiesTheUser() throws Exception {
    api.signIn(organization.administratorEmail().toUpperCase(), PASSWORD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresAt").isNotEmpty())
        .andExpect(jsonPath("$.userId").value(organization.administratorId()))
        .andExpect(jsonPath("$.organizationId").value(organization.id()))
        .andExpect(jsonPath("$.role").value("ORGANIZATION_ADMIN"))
        .andExpect(jsonPath("$.fullName").value("Carlos Huamán"));

    mockMvc
        .perform(get(ME).with(bearer(administratorToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(organization.administratorEmail()))
        .andExpect(jsonPath("$.lastLoginAt").isNotEmpty());
  }

  @Test
  void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
    api.signIn(organization.administratorEmail(), "Otra-clave-2026")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Invalid credentials."));
    api.signIn(uniqueEmail(), PASSWORD)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid credentials."));
    api.signIn("", "").andExpect(status().isBadRequest());
  }

  @Test
  void fifthFailedSignInLocksTheAccountUntilTheAdministratorUnlocksIt() throws Exception {
    String email = uniqueEmail();
    String userId = api.registeredUserId(administratorToken, organization.id(), email, "AUTHORITY");
    for (int attempt = 1; attempt <= 5; attempt++) {
      api.signIn(email, "Otra-clave-2026").andExpect(status().isUnauthorized());
    }

    api.signIn(email, PASSWORD).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            get("/api/v1/organizations/" + organization.id() + "/users")
                .with(bearer(administratorToken)))
        .andExpect(jsonPath("$[?(@.id == '" + userId + "')].status").value(contains("LOCKED")));

    api.changeUser(administratorToken, userId, "status", "ACTIVE")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    api.signIn(email, PASSWORD).andExpect(status().isOk());
  }

  @Test
  void tokenOfADisabledUserStopsWorkingAtOnce() throws Exception {
    String email = uniqueEmail();
    String userId = api.registeredUserId(administratorToken, organization.id(), email, "AUTHORITY");
    String token = api.tokenOf(email);
    mockMvc.perform(get(ME).with(bearer(token))).andExpect(status().isOk());

    api.changeUser(administratorToken, userId, "status", "DISABLED")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISABLED"));

    mockMvc.perform(get(ME).with(bearer(token))).andExpect(status().isUnauthorized());
    api.signIn(email, PASSWORD).andExpect(status().isUnauthorized());
  }

  @Test
  void roleChangeAppliesToTheNextRequestWithTheSameToken() throws Exception {
    String email = uniqueEmail();
    String userId = api.registeredUserId(administratorToken, organization.id(), email, "AUTHORITY");
    String token = api.tokenOf(email);
    String users = "/api/v1/organizations/" + organization.id() + "/users";
    mockMvc.perform(get(users).with(bearer(token))).andExpect(status().isForbidden());

    api.changeUser(administratorToken, userId, "role", "ORGANIZATION_ADMIN")
        .andExpect(status().isOk());

    mockMvc.perform(get(users).with(bearer(token))).andExpect(status().isOk());
  }

  @Test
  void missingOrInvalidTokenReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(get(ME))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Sign in to use this endpoint."));
    mockMvc
        .perform(get(ME).with(bearer(administratorToken + "x")))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get(ME).with(bearer("not-a-jwt"))).andExpect(status().isUnauthorized());
  }
}
