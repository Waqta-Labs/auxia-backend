package com.waqtalabs.auxia;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Signed-in users for MockMvc requests, with the same principal that the JWT filter puts in the
 * security context. A request made with one of them replaces the default user of {@link
 * DefaultAuthorityUserConfiguration}.
 */
public final class TestUsers {

  private TestUsers() {}

  public static RequestPostProcessor authority() {
    return user(UUID.randomUUID(), "AUTHORITY");
  }

  public static RequestPostProcessor fieldBrigade() {
    return user(UUID.randomUUID(), "FIELD_BRIGADE");
  }

  public static RequestPostProcessor auditor() {
    return user(UUID.randomUUID(), "AUDITOR");
  }

  public static RequestPostProcessor organizationAdmin(UUID organizationId) {
    return user(organizationId, "ORGANIZATION_ADMIN");
  }

  public static RequestPostProcessor member(UUID organizationId, String role) {
    return user(organizationId, role);
  }

  private static RequestPostProcessor user(UUID organizationId, String role) {
    var principal = new AuthenticatedUser(UUID.randomUUID(), organizationId, role);
    return authentication(
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }
}
