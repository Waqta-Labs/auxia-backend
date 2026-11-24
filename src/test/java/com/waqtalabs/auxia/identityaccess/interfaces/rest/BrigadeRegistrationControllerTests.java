package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.waqtalabs.auxia.SignedInOrganization;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** A brigadist registers from the app (A_02), is approved and signs in with the phone secret. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BrigadeRegistrationControllerTests {

  private static final String SECRET = "c2VjcmV0by1kZWwtdGVsZWZvbm8tZGUtbWlndWVsLTAx";

  @Autowired private MockMvc mockMvc;

  private SignedInOrganization organization;
  private String code;

  @BeforeEach
  void createBrigade() throws Exception {
    organization = SignedInOrganization.register(mockMvc);
    code = "BRG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    mockMvc
        .perform(
            post("/api/v1/brigades")
                .with(organization.administrator())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Brigada Alfa", "code": "%s",
                     "baseLocation": {"latitude": -11.9436, "longitude": -76.7078}}
                    """
                        .formatted(code.toLowerCase())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void approvedBrigadistSignsInWithThePhoneSecretAndSeesTheBrigade() throws Exception {
    String id = UUID.randomUUID().toString();

    submit(id, code, SECRET)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.reason").value(nullValue()));
    submit(id, code, SECRET).andExpect(status().isOk());
    submit(id, code, SECRET.replace('c', 'd')).andExpect(status().isConflict());
    statusOf(id, SECRET).andExpect(jsonPath("$.status").value("PENDING"));
    statusOf(id, SECRET.replace('c', 'd')).andExpect(status().isNotFound());
    deviceSignIn(id).andExpect(status().isUnauthorized());

    mockMvc
        .perform(get("/api/v1/brigade-registrations").with(organization.administrator()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].brigadeCode").value(code))
        .andExpect(jsonPath("$[0].hasIdCard").value(true));
    mockMvc
        .perform(
            get("/api/v1/brigade-registrations/" + id + "/id-card")
                .with(organization.administrator()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(content().bytes("carnet".getBytes(StandardCharsets.UTF_8)));
    mockMvc
        .perform(
            post("/api/v1/brigade-registrations/" + id + "/approval")
                .with(organization.authority()))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/v1/brigade-registrations/" + id + "/approval")
                .with(organization.administrator()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.userId").value(notNullValue()));
    mockMvc
        .perform(
            post("/api/v1/brigade-registrations/" + id + "/approval")
                .with(organization.administrator()))
        .andExpect(status().isConflict());
    statusOf(id, SECRET).andExpect(jsonPath("$.status").value("APPROVED"));

    String token =
        JsonPath.read(
            deviceSignIn(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FIELD_BRIGADE"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.token");
    mockMvc
        .perform(get("/api/v1/me/missions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brigadeName").value("Brigada Alfa"));
    mockMvc
        .perform(get("/api/v1/brigades?status=AVAILABLE").with(organization.administrator()))
        .andExpect(jsonPath("$[0].members[*].fullName").value(hasItem("Miguel Quispe")));
  }

  @Test
  void rejectedRequestShowsItsReasonAndUnknownCodesAreNotReceived() throws Exception {
    String id = UUID.randomUUID().toString();
    submit(id, code, SECRET).andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/brigade-registrations/" + id + "/rejection")
                .with(organization.administrator())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"El código no es de tu organismo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));

    statusOf(id, SECRET)
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.reason").value("El código no es de tu organismo"));
    deviceSignIn(id).andExpect(status().isUnauthorized());
    submit(UUID.randomUUID().toString(), "BRG-NO-EXISTE", SECRET).andExpect(status().isNotFound());
  }

  private ResultActions submit(String id, String brigadeCode, String secret) throws Exception {
    return mockMvc.perform(
        multipart(HttpMethod.PUT, "/api/v1/public/brigade-registrations/" + id)
            .file(
                new MockMultipartFile(
                    "registration",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    """
                    {"fullName": "Miguel Quispe", "phone": "987654321",
                     "organization": "Defensa Civil", "brigadeCode": "%s", "deviceSecret": "%s"}
                    """
                        .formatted(brigadeCode, secret)
                        .getBytes(StandardCharsets.UTF_8)))
            .file(
                new MockMultipartFile(
                    "idCard",
                    "carnet.jpg",
                    "image/jpeg",
                    "carnet".getBytes(StandardCharsets.UTF_8))));
  }

  private ResultActions statusOf(String id, String secret) throws Exception {
    return mockMvc.perform(
        get("/api/v1/public/brigade-registrations/" + id).header("X-Device-Secret", secret));
  }

  private ResultActions deviceSignIn(String id) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/device-sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"registrationId\": \"%s\", \"deviceSecret\": \"%s\"}".formatted(id, SECRET)));
  }
}
