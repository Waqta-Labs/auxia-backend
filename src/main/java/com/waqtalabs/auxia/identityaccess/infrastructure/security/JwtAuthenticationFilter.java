package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.identityaccess.application.queries.GetActiveUserAccountQuery;
import com.waqtalabs.auxia.identityaccess.application.queryservices.CheckAccessQueryHandler;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token of each request, of any bounded context, and leaves its user in the
 * security context with the authority {@code ROLE_<role>}. A request without a token stays
 * anonymous; one with an invalid or expired token too, so a protected endpoint answers 401.
 *
 * <p>The role and the organization come from the current account, not from the token: a user
 * whose account or organization is no longer active stays anonymous, and a role change applies
 * to the next request (R-05).
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER = "Bearer ";

  private final JwtTokenVerifier verifier;
  private final CheckAccessQueryHandler checkAccess;

  JwtAuthenticationFilter(JwtTokenVerifier verifier, CheckAccessQueryHandler checkAccess) {
    this.verifier = verifier;
    this.checkAccess = checkAccess;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)) {
      authenticate(header.substring(BEARER.length()))
          .ifPresentOrElse(
              user -> {
                var authentication =
                    new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
              },
              SecurityContextHolder::clearContext);
    }
    chain.doFilter(request, response);
  }

  private Optional<AuthenticatedUser> authenticate(String token) {
    AuthenticatedUser claims;
    try {
      claims = verifier.verify(token);
    } catch (JwtException ex) {
      LOGGER.debug("Rejected token: {}", ex.getMessage());
      return Optional.empty();
    }
    Optional<UserAccount> account =
        checkAccess.handle(new GetActiveUserAccountQuery(new UserId(claims.userId())));
    if (account.isEmpty()) {
      LOGGER.debug("Rejected token of user {}: account or organization inactive", claims.userId());
    }
    return account.map(
        current ->
            new AuthenticatedUser(
                current.id().value(),
                current.organizationId().value(),
                current.role().name()));
  }
}
