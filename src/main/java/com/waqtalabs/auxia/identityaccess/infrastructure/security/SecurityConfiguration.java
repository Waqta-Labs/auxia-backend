package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.identityaccess.application.queryservices.CheckAccessQueryHandler;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Security of the whole Backend API. Every endpoint needs a valid JWT except the sign-in, the
 * organization endpoints of the platform operator (which check X-Platform-Key), the public view
 * and the SOS of citizens, the API documentation and the health check. Roles are checked on each
 * controller method with {@code @PreAuthorize}.
 *
 * <p>A missing token (401) and a missing role (403) are answered by the global exception handler,
 * so they share the problem detail format and the language of every other error.
 *
 * <p>The web application runs in the browser from another domain, so the API answers CORS
 * requests from the origins in {@code auxia.web.allowed-origins}. The field app is not a browser
 * and does not need it.
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
        .cors(Customizer.withDefaults())
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
                    // Anyone can verify a delivery with its public code (US-17, US-26).
                    .requestMatchers(HttpMethod.GET, "/api/v1/deliveries/*/verify")
                    .permitAll()
                    // Public view for citizens (EP-08).
                    .requestMatchers(HttpMethod.GET, "/api/v1/public/**")
                    .permitAll()
                    // SOS of citizens from the mobile app, limited per address.
                    .requestMatchers(HttpMethod.POST, "/api/v1/public/field-reports")
                    .permitAll()
                    // The SINPAD simulator checks the key of the sender (X-Api-Key).
                    .requestMatchers("/sinpad-simulator/**")
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

  /**
   * The token travels in the Authorization header, never in a cookie, so the browser does not
   * send credentials. The web app reads the file name of the PDF and CSV exports and the location
   * of what it creates.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${auxia.web.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(allowedOrigins);
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT_LANGUAGE));
    cors.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.LOCATION));
    cors.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }
}
