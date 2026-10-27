package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER = "Bearer ";

  private final JwtTokenVerifier verifier;

  JwtAuthenticationFilter(JwtTokenVerifier verifier) {
    this.verifier = verifier;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)) {
      try {
        AuthenticatedUser user = verifier.verify(header.substring(BEARER.length()));
        var authentication =
            new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException ex) {
        LOGGER.debug("Rejected token: {}", ex.getMessage());
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
