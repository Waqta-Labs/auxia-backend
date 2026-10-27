package com.waqtalabs.auxia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Every MockMvc request is made by a signed-in AUTHORITY unless the request sets another user
 * with {@link TestUsers}, or {@code anonymous()} to check that the token is required.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DefaultAuthorityUserConfiguration {

  @Bean
  MockMvcBuilderCustomizer defaultAuthorityUser() {
    return builder -> builder.defaultRequest(get("/").with(TestUsers.authority()));
  }
}
