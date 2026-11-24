package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** The web application calls the API from the browser, from the origins of the configuration. */
@SpringBootTest(properties = "auxia.web.allowed-origins=https://app.auxia.test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorsTests {

  private static final String WEB_APP = "https://app.auxia.test";

  @Autowired private MockMvc mockMvc;

  @Test
  void preflightOfTheWebAppIsAllowedWithoutAToken() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/emergencies")
                .header(HttpHeaders.ORIGIN, WEB_APP)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization, content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEB_APP))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    containsStringIgnoringCase("authorization")));
  }

  @Test
  void responseLetsTheWebAppReadTheFileNameOfAnExport() throws Exception {
    mockMvc
        .perform(get("/api/v1/public/emergencies").header(HttpHeaders.ORIGIN, WEB_APP))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEB_APP))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    containsString(HttpHeaders.CONTENT_DISPOSITION)));
  }

  @Test
  void preflightOfAnotherOriginIsRejected() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/emergencies")
                .header(HttpHeaders.ORIGIN, "https://other-site.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }
}
