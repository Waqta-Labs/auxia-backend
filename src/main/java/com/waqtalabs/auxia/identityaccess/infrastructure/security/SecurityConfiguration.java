package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.identityaccess.application.queryservices.CheckAccessQueryHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Security of the whole Backend API. Every endpoint needs a valid JWT except the sign-in, the
 * organization endpoints of the platform operator (which check X-Platform-Key), the API
 * documentation and the health check. Roles are checked on each controller method with
 * {@code @PreAuthorize}.
 *
 * <p>A missing token (401) and a missing role (403) are answered by the global exception handler,
 * so they share the problem detail format and the language of every other error.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenVerifier verifier,
      CheckAccessQueryHandler checkAccess,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/auth/sign-in", "/api/v1/organizations")
                    .permitAll()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/organizations/*/status")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, ex) ->
                            exceptionResolver.resolveException(request, response, null, ex))
                    .accessDeniedHandler(
                        (request, response, ex) ->
                            exceptionResolver.resolveException(request, response, null, ex)))
        .addFilterBefore(
            new JwtAuthenticationFilter(verifier, checkAccess),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
