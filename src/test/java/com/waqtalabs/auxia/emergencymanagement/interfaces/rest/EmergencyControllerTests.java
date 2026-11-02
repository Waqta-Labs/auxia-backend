package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static com.waqtalabs.auxia.TestUsers.auditor;
import static com.waqtalabs.auxia.TestUsers.fieldBrigade;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.DefaultAuthorityUserConfiguration;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DefaultAuthorityUserConfiguration.class})
@Transactional
class EmergencyControllerTests {

  static final String EMERGENCIES = "/api/v1/emergencies";

  static final String CHOSICA_EMERGENCY =
      """
      {
        "name": "Huaico Chosica 2026",
        "type": "LANDSLIDE",
        "description": "Huaico tras lluvias intensas",
        "location": {
          "department": "Lima",
          "province": "Lima",
          "district": "Lurigancho-Chosica"
        },
        "startDate": "2026-03-14"
      }
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void registerReturnsCreatedWithTheActiveEmergency() throws Exception {
    mockMvc
        .perform(
            post(EMERGENCIES).contentType(MediaType.APPLICATION_JSON).content(CHOSICA_EMERGENCY))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, startsWith(EMERGENCIES + "/")))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.name").value("Huaico Chosica 2026"))
        .andExpect(jsonPath("$.type").value("LANDSLIDE"))
        .andExpect(jsonPath("$.location.district").value("Lurigancho-Chosica"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.startDate").value("2026-03-14"));
  }

  @Test
  void registerWithoutRequiredFieldsReturnsBadRequestWithTheInvalidFields() throws Exception {
    mockMvc
        .perform(
            post(EMERGENCIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\", \"location\": {\"department\": \"Lima\"}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("type")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("location.province")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("startDate")));
  }

  @Test
  void registerWithAFutureStartDateReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(EMERGENCIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHOSICA_EMERGENCY.replace("2026-03-14", "2999-01-01")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("startDate"));
  }

  @Test
  void registerWithAnUnknownTypeReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post(EMERGENCIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHOSICA_EMERGENCY.replace("LANDSLIDE", "TSUNAMI")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findByIdReturnsTheEmergency() throws Exception {
    String id = register();

    mockMvc
        .perform(get(EMERGENCIES + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.description").value("Huaico tras lluvias intensas"));
  }

  @Test
  void unknownEmergencyReturnsNotFoundInTheLanguageOfTheRequest() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(get(EMERGENCIES + "/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not found"))
        .andExpect(jsonPath("$.detail").value("Emergency " + id + " does not exist."));
    mockMvc
        .perform(get(EMERGENCIES + "/" + id).header(HttpHeaders.ACCEPT_LANGUAGE, "es-PE"))
        .andExpect(jsonPath("$.title").value("No encontrado"))
        .andExpect(jsonPath("$.detail").value("La emergencia " + id + " no existe."));
  }

  @Test
  void malformedIdReturnsBadRequest() throws Exception {
    mockMvc.perform(get(EMERGENCIES + "/not-a-uuid")).andExpect(status().isBadRequest());
  }

  @Test
  void closeClosesTheEmergencyAndRemovesItFromTheActiveList() throws Exception {
    String id = register();

    mockMvc
        .perform(patch(EMERGENCIES + "/" + id + "/close"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
    mockMvc
        .perform(get(EMERGENCIES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(not(hasItem(id))));
  }

  @Test
  void activeEmergenciesIncludeARegisteredOne() throws Exception {
    String id = register();

    mockMvc
        .perform(get(EMERGENCIES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(hasItem(id)));
  }

  @Test
  void closingTwiceReturnsConflict() throws Exception {
    String id = register();
    mockMvc.perform(patch(EMERGENCIES + "/" + id + "/close")).andExpect(status().isOk());

    mockMvc
        .perform(patch(EMERGENCIES + "/" + id + "/close"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Emergency " + id + " is already closed."));
  }

  @Test
  void onlyAnAuthorityChangesEmergenciesAndAnySignedInUserReadsThem() throws Exception {
    String id = register();

    mockMvc
        .perform(
            post(EMERGENCIES)
                .with(fieldBrigade())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHOSICA_EMERGENCY))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(patch(EMERGENCIES + "/" + id + "/close").with(auditor()))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(EMERGENCIES + "/" + id).with(auditor())).andExpect(status().isOk());
    mockMvc
        .perform(get(EMERGENCIES).with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Unauthorized"));
  }

  private String register() throws Exception {
    String body =
        mockMvc
            .perform(
                post(EMERGENCIES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CHOSICA_EMERGENCY))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }
}
