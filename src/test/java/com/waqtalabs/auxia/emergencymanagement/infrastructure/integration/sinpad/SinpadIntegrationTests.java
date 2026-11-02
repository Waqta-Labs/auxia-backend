package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.sinpad;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SinpadGateway;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The field reports of an emergency go to SINPAD by HTTP, as to any external system: here to the
 * SINPAD simulator served on the random port of the test (US-33).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  SinpadIntegrationTests.SimulatorOnTheTestPort.class
})
class SinpadIntegrationTests {

  private static final String KEY = "sinpad-simulator-local-key";

  @Autowired private MockMvc mockMvc;
  @Autowired private FakeAiService aiService;
  @Autowired private SinpadExportQueryService exports;
  @Autowired private MessageSource messages;
  @LocalServerPort private int port;

  private SignedInOrganization organization;
  private String emergencyId;

  @BeforeEach
  void emergencyWithACompleteReport() throws Exception {
    aiService.reset();
    organization = SignedInOrganization.register(mockMvc);
    emergencyId =
        idOf(
            send(
                post("/api/v1/emergencies"),
                """
                {
                  "name": "Huaico Chosica 2026",
                  "type": "LANDSLIDE",
                  "location": {"department": "Lima", "province": "Lima",
                               "district": "Lurigancho-Chosica"},
                  "startDate": "2026-03-14"
                }
                """));
    String zoneId =
        idOf(
            send(
                post("/api/v1/emergencies/" + emergencyId + "/zones"),
                """
                {
                  "name": "Sector Alto Perú",
                  "populationImpact": {"population": 600, "familiesAffected": 35,
                                       "peopleAffected": 140},
                  "damageLevel": "SEVERE"
                }
                """));
    String text = "Huaico en Alto Perú " + UUID.randomUUID();
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.SEVERE, 35, 140, null, null, null, Set.of(Need.WATER), List.of()));
    send(
            post("/api/v1/zones/" + zoneId + "/field-reports"),
            "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text))
        .andExpect(status().isCreated());
  }

  @Test
  void reportsReachSinpadInSpanishAndKeepTheirNumberWhenSentAgain() throws Exception {
    String code =
        JsonPath.read(
            submit()
                .andExpect(status().isCreated())
                .andExpect(
                    jsonPath("$.registrationCode").value(matchesPattern("SINPAD-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.reportsReceived").value(1))
                .andExpect(jsonPath("$.receivedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.registrationCode");

    mockMvc
        .perform(get("/sinpad-simulator/api/v1/reportes/" + code).header("X-Api-Key", KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigoExterno").value(emergencyId))
        .andExpect(jsonPath("$.emergencia.tipoFenomeno").value("Huaico o deslizamiento"))
        .andExpect(jsonPath("$.emergencia.distrito").value("Lurigancho-Chosica"))
        .andExpect(jsonPath("$.reportes[0].zona").value("Sector Alto Perú"))
        .andExpect(jsonPath("$.reportes[0].fuente").value("Autoridad"))
        .andExpect(jsonPath("$.reportes[0].nivelDano").value("Grave"))
        .andExpect(jsonPath("$.reportes[0].personasAfectadas").value(140))
        .andExpect(jsonPath("$.reportes[0].familiasAfectadas").value(35))
        .andExpect(jsonPath("$.reportes[0].necesidades").value(contains("Agua")));
    submit().andExpect(status().isCreated()).andExpect(jsonPath("$.registrationCode").value(code));
  }

  @Test
  void onlyAnAuthoritySendsTheReports() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/emergencies/" + emergencyId + "/sinpad-submissions")
                .with(organization.auditor()))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinpadThatDoesNotAnswerOrRejectsTheKeyIsUnavailable() {
    var export = exports.export(new EmergencyId(UUID.fromString(emergencyId)));

    assertThatThrownBy(() -> gateway(URI.create("http://localhost:1/sinpad"), KEY).submit(export))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    assertThatThrownBy(() -> gateway(simulator(port), "another-key").submit(export))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  private ResultActions submit() throws Exception {
    return mockMvc.perform(
        post("/api/v1/emergencies/" + emergencyId + "/sinpad-submissions")
            .with(organization.authority()));
  }

  private RestSinpadGateway gateway(URI baseUrl, String key) {
    return new RestSinpadGateway(
        new SinpadProperties(baseUrl, key, Duration.ofSeconds(2), Duration.ofSeconds(5)),
        messages);
  }

  private static URI simulator(Object port) {
    return URI.create("http://localhost:" + port + "/sinpad-simulator");
  }

  private ResultActions send(MockHttpServletRequestBuilder request, String body)
      throws Exception {
    return mockMvc.perform(
        request
            .with(organization.authority())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }

  /**
   * The gateway of the application, aimed at the simulator on the port of the test, which is
   * known only once the server is up.
   */
  @TestConfiguration(proxyBeanMethods = false)
  static class SimulatorOnTheTestPort {

    @Bean
    @Primary
    SinpadGateway simulatorOnTheTestPort(Environment environment, MessageSource messages) {
      return export ->
          new RestSinpadGateway(
                  new SinpadProperties(
                      simulator(environment.getProperty("local.server.port")),
                      KEY,
                      Duration.ofSeconds(2),
                      Duration.ofSeconds(10)),
                  messages)
              .submit(export);
    }
  }
}
