package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.PASSWORD;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.PLATFORM_KEY;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.bearer;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.organizationRequest;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.uniqueEmail;
import static com.waqtalabs.auxia.identityaccess.interfaces.rest.IdentityApi.uniqueTaxId;
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
class OrganizationControllerTests {

  static final String ORGANIZATIONS = "/api/v1/organizations";

  @Autowired private MockMvc mockMvc;

  private IdentityApi api;

  @BeforeEach
  void createApi() {
    api = new IdentityApi(mockMvc);
  }

  @Test
  void platformOperatorRegistersAnOrganizationWithItsFirstAdministrator() throws Exception {
    String taxId = uniqueTaxId();
    String email = uniqueEmail();

    api.registerOrganization(organizationRequest(taxId, email), PLATFORM_KEY)
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith(ORGANIZATIONS + "/")))
        .andExpect(jsonPath("$.organization.id").isNotEmpty())
        .andExpect(jsonPath("$.organization.name").value("COER Lima"))
        .andExpect(jsonPath("$.organization.type").value("GOVERNMENT"))
        .andExpect(jsonPath("$.organization.taxId").value(taxId))
        .andExpect(jsonPath("$.organization.status").value("ACTIVE"))
        .andExpect(jsonPath("$.administrator.email").value(email))
        .andExpect(jsonPath("$.administrator.role").value("ORGANIZATION_ADMIN"))
        .andExpect(jsonPath("$.administrator.status").value("ACTIVE"))
        .andExpect(jsonPath("$.administrator.password").doesNotExist())
        .andExpect(jsonPath("$.administrator.passwordHash").doesNotExist());
    api.signIn(email, PASSWORD).andExpect(status().isOk());
  }

  @Test
  void registrationWithoutTheRightPlatformKeyIsUnauthorized() throws Exception {
    String request = organizationRequest(uniqueTaxId(), uniqueEmail());

    api.registerOrganization(request, null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Invalid credentials."));
    api.registerOrganization(request, PLATFORM_KEY + "x").andExpect(status().isUnauthorized());
  }

  @Test
  void repeatedTaxIdOrAdministratorEmailReturnsConflict() throws Exception {
    String taxId = uniqueTaxId();
    String email = uniqueEmail();
    api.registerOrganization(organizationRequest(taxId, email), PLATFORM_KEY)
        .andExpect(status().isCreated());

    api.registerOrganization(organizationRequest(taxId, uniqueEmail()), PLATFORM_KEY)
        .andExpect(status().isConflict());
    api.registerOrganization(organizationRequest(uniqueTaxId(), email), PLATFORM_KEY)
        .andExpect(status().isConflict());
  }

  @Test
  void invalidOrganizationReturnsBadRequest() throws Exception {
    api.registerOrganization(organizationRequest("2013137", "not-an-email"), PLATFORM_KEY)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("taxId")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("administrator.email")));
  }

  @Test
  void signedInUserSeesOnlyTheirOwnOrganization() throws Exception {
    RegisteredOrganization own = api.registerOrganization();
    RegisteredOrganization other = api.registerOrganization();
    String token = api.tokenOf(own.administratorEmail());

    mockMvc
        .perform(get(ORGANIZATIONS + "/" + own.id()).with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(own.id()));
    mockMvc
        .perform(get(ORGANIZATIONS + "/" + other.id()).with(bearer(token)))
        .andExpect(status().isNotFound());
  }

  @Test
  void suspendedOrganizationCannotSignInAndItsTokensStopWorking() throws Exception {
    RegisteredOrganization organization = api.registerOrganization();
    String token = api.tokenOf(organization.administratorEmail());

    api.changeOrganizationStatus(organization.id(), "SUSPENDED")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUSPENDED"));

    mockMvc
        .perform(get("/api/v1/users/me").with(bearer(token)))
        .andExpect(status().isUnauthorized());
    api.signIn(organization.administratorEmail(), PASSWORD).andExpect(status().isUnauthorized());

    api.changeOrganizationStatus(organization.id(), "ACTIVE").andExpect(status().isOk());
    api.signIn(organization.administratorEmail(), PASSWORD).andExpect(status().isOk());
  }
}
