package com.waqtalabs.auxia.sinpadsimulator;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The SINPAD simulator takes the reports of an emergency with the key of the sender. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SinpadSimulatorControllerTests {

  private static final String REPORTS = "/sinpad-simulator/api/v1/reportes";
  private static final String KEY = "sinpad-simulator-local-key";

  @Autowired private MockMvc mockMvc;

  @Test
  void emergencyIsRegisteredOnceAndItsReportsAreReplacedWhenSentAgain() throws Exception {
    String emergency = UUID.randomUUID().toString();

    String code =
        JsonPath.read(
            send(KEY, submission(emergency, 140))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoExterno").value(emergency))
                .andExpect(jsonPath("$.estado").value("RECIBIDO"))
                .andExpect(jsonPath("$.reportesRecibidos").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.codigoRegistro");

    send(KEY, submission(emergency, 400))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigoRegistro").value(code));
    mockMvc
        .perform(get(REPORTS + "/" + code).header("X-Api-Key", KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emergencia.distrito").value("Lurigancho-Chosica"))
        .andExpect(jsonPath("$.reportes[0].personasAfectadas").value(400));
  }

  @Test
  void anotherEmergencyGetsAnotherNumber() throws Exception {
    String first =
        JsonPath.read(
            send(KEY, submission(UUID.randomUUID().toString(), 10))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.codigoRegistro");

    send(KEY, submission(UUID.randomUUID().toString(), 10))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoRegistro").value(not(first)));
  }

  @Test
  void reportWithoutTheFamiliesAffectedIsRejected() throws Exception {
    String withoutFamilies =
        submission(UUID.randomUUID().toString(), 140).replace("\"familiasAfectadas\": 35,", "");

    send(KEY, withoutFamilies).andExpect(status().isBadRequest());
  }

  @Test
  void wrongOrMissingKeyIsUnauthorized() throws Exception {
    send("another-key", submission(UUID.randomUUID().toString(), 140))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post(REPORTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(UUID.randomUUID().toString(), 140)))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get(REPORTS + "/SINPAD-2026-000001"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownRegistrationIsNotFound() throws Exception {
    mockMvc
        .perform(get(REPORTS + "/SINPAD-1999-999999").header("X-Api-Key", KEY))
        .andExpect(status().isNotFound());
  }

  private ResultActions send(String key, String body) throws Exception {
    return mockMvc.perform(
        post(REPORTS)
            .header("X-Api-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static String submission(String emergency, int people) {
    return """
        {
          "codigoExterno": "%s",
          "emergencia": {
            "nombre": "Huaico Chosica 2026",
            "tipoFenomeno": "Huaico o deslizamiento",
            "departamento": "Lima",
            "provincia": "Lima",
            "distrito": "Lurigancho-Chosica",
            "fechaInicio": "2026-03-14"
          },
          "reportes": [{
            "zona": "Sector Alto Perú",
            "fechaReporte": "2026-03-15T09:00:00",
            "fuente": "Autoridad",
            "nivelDano": "Grave",
            "personasAfectadas": %d,
            "familiasAfectadas": 35,
            "necesidades": ["Agua"]
          }]
        }
        """
        .formatted(emergency, people);
  }
}
