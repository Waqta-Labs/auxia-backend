package com.waqtalabs.auxia;

import static com.waqtalabs.auxia.TestUsers.auditor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The health check is public, but its components are internal details of the server. */
class HealthEndpointTests {

  static final String HEALTH = "/actuator/health";

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @Import(TestcontainersConfiguration.class)
  class DevProfile {

    @Autowired private MockMvc mockMvc;

    @Test
    void anonymousRequestGetsOnlyTheStatus() throws Exception {
      mockMvc
          .perform(get(HEALTH))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void signedInUserGetsTheComponents() throws Exception {
      mockMvc
          .perform(get(HEALTH).with(auditor()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.components.db.status").value("UP"))
          .andExpect(jsonPath("$.components.diskSpace").exists());
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @Import(TestcontainersConfiguration.class)
  @ActiveProfiles("prod")
  class ProdProfile {

    @Autowired private MockMvc mockMvc;

    @Test
    void signedInUserGetsOnlyTheStatus() throws Exception {
      mockMvc
          .perform(get(HEALTH).with(auditor()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").doesNotExist());
    }
  }
}
