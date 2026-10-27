package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.PASSWORD;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.bearer;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.uniqueEmail;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UserAccountControllerTests {

  @Autowired private MockMvc mockMvc;

  private IdentityApi api;
  private RegisteredOrganization organization;
  private String administratorToken;
  private String users;

  @BeforeEach
  void registerOrganization() throws Exception {
    api = new IdentityApi(mockMvc);
    organization = api.registerOrganization();
    administratorToken = api.tokenOf(organization.administratorEmail());
    users = "/api/v1/organizations/" + organization.id() + "/users";
  }

  @Test
  void administratorRegistersAUserWhoSignsInWithTheirRole() throws Exception {
    String email = uniqueEmail();

    api.registerUser(administratorToken, organization.id(), email, "FIELD_BRIGADE")
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/users/")))
        .andExpect(jsonPath("$.organizationId").value(organization.id()))
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("FIELD_BRIGADE"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.lastLoginAt").isEmpty());

    api.signIn(email, PASSWORD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("FIELD_BRIGADE"))
        .andExpect(jsonPath("$.organizationId").value(organization.id()));
  }

  @Test
  void administratorListsTheUsersOfTheOrganizationByEmail() throws Exception {
    api.registeredUserId(administratorToken, organization.id(), "b-" + uniqueEmail(), "AUDITOR");
    api.registeredUserId(administratorToken, organization.id(), "a-" + uniqueEmail(), "AUDITOR");

    mockMvc
        .perform(get(users).with(bearer(administratorToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].email").value(startsWith("a-")))
        .andExpect(jsonPath("$[1].email").value(startsWith("b-")));
  }

  @Test
  void repeatedEmailReturnsConflictAndInvalidUserReturnsBadRequest() throws Exception {
    api.registerUser(
            administratorToken, organization.id(), organization.administratorEmail(), "AUDITOR")
        .andExpect(status().isConflict());
    api.registerUser(administratorToken, organization.id(), "not-an-email", "UNKNOWN")
        .andExpect(status().isBadRequest());
  }

  @Test
  void administratorChangesTheRoleOfAUser() throws Exception {
    String userId =
        api.registeredUserId(administratorToken, organization.id(), uniqueEmail(), "AUTHORITY");

    api.changeUser(administratorToken, userId, "role", "AUDITOR")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("AUDITOR"));
    mockMvc
        .perform(get(users).with(bearer(administratorToken)))
        .andExpect(jsonPath("$[?(@.id == '" + userId + "')].role").value(contains("AUDITOR")));
  }

  @Test
  void administratorCannotManageTheirOwnAccountNorLockAnAccountByHand() throws Exception {
    String userId =
        api.registeredUserId(administratorToken, organization.id(), uniqueEmail(), "AUTHORITY");

    api.changeUser(administratorToken, organization.administratorId(), "role", "AUDITOR")
        .andExpect(status().isUnprocessableContent());
    api.changeUser(administratorToken, organization.administratorId(), "status", "DISABLED")
        .andExpect(status().isUnprocessableContent());
    api.changeUser(administratorToken, userId, "status", "LOCKED")
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void administratorOfAnotherOrganizationCannotSeeNorManageItsUsers() throws Exception {
    String userId =
        api.registeredUserId(administratorToken, organization.id(), uniqueEmail(), "AUTHORITY");
    RegisteredOrganization other = api.registerOrganization();
    String otherToken = api.tokenOf(other.administratorEmail());

    mockMvc.perform(get(users).with(bearer(otherToken))).andExpect(status().isNotFound());
    api.registerUser(otherToken, organization.id(), uniqueEmail(), "AUTHORITY")
        .andExpect(status().isNotFound());
    api.changeUser(otherToken, userId, "role", "AUDITOR").andExpect(status().isNotFound());
    api.changeUser(otherToken, userId, "status", "DISABLED").andExpect(status().isNotFound());
  }

  @Test
  void otherRolesCannotManageUsersButSeeTheirOwnAccount() throws Exception {
    String email = uniqueEmail();
    api.registeredUserId(administratorToken, organization.id(), email, "AUTHORITY");
    String token = api.tokenOf(email);

    api.registerUser(token, organization.id(), uniqueEmail(), "AUTHORITY")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Forbidden"));
    mockMvc.perform(get(users).with(bearer(token))).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/users/me").with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("AUTHORITY"));
  }

  @Test
  void invalidRoleOrStatusReturnsBadRequest() throws Exception {
    String userId =
        api.registeredUserId(administratorToken, organization.id(), uniqueEmail(), "AUTHORITY");

    api.changeUser(administratorToken, userId, "role", "ROOT")
        .andExpect(status().isBadRequest());
    api.registerUser(administratorToken, organization.id(), "", "AUTHORITY")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")));
  }
}
