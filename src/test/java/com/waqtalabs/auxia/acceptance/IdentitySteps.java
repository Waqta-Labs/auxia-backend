package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Steps of US-21 and US-22. They sign in through the API and send the token they get, without the
 * default user of the other scenarios, so the JWT filter decides who the user is.
 */
public class IdentitySteps {

  private static final String PASSWORD = "Chosica-2026";
  private static final String PLATFORM_KEY = "test-platform-key-0123456789";

  @Autowired private MockMvc mockMvc;

  private String administratorToken;
  private String userEmail;
  private String userId;
  private String userToken;
  private MockHttpServletResponse response;
  private MockHttpServletResponse otherResponse;

  @Dado("una organización con un usuario de rol {string}")
  public void anOrganizationWithAUser(String role) throws Exception {
    String administratorEmail = uniqueEmail();
    response =
        send(
            post("/api/v1/organizations").header("X-Platform-Key", PLATFORM_KEY),
            """
            {
              "name": "COER Lima",
              "type": "GOVERNMENT",
              "taxId": "20%d",
              "administrator": {
                "email": "%s",
                "firstName": "Carlos",
                "lastName": "Huamán",
                "password": "%s"
              }
            }
            """
                .formatted(
                    ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L),
                    administratorEmail,
                    PASSWORD));
    assertThat(response.getStatus()).isEqualTo(201);
    String organizationId = JsonPath.read(response.getContentAsString(), "$.organization.id");
    administratorToken = tokenOf(signIn(administratorEmail, PASSWORD));

    userEmail = uniqueEmail();
    response =
        send(
            post("/api/v1/organizations/" + organizationId + "/users"),
            administratorToken,
            """
            {
              "email": "%s",
              "firstName": "Rosa",
              "lastName": "Quispe",
              "role": "%s",
              "password": "%s"
            }
            """
                .formatted(userEmail, role, PASSWORD));
    assertThat(response.getStatus()).isEqualTo(201);
    userId = JsonPath.read(response.getContentAsString(), "$.id");
  }

  @Dado("que el usuario inició sesión con su contraseña")
  @Cuando("el usuario inicia sesión con su contraseña")
  public void theUserSignsIn() throws Exception {
    response = signIn(userEmail, PASSWORD);
    userToken = tokenOf(response);
  }

  @Dado("que el administrador cambia el rol del usuario a {string}")
  public void theAdministratorChangesTheRole(String role) throws Exception {
    response =
        send(
            patch("/api/v1/users/" + userId + "/role"),
            administratorToken,
            "{\"role\": \"" + role + "\"}");
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Cuando("el usuario inicia sesión con una contraseña incorrecta")
  public void theUserSignsInWithAWrongPassword() throws Exception {
    response = signIn(userEmail, "Otra-clave-2026");
  }

  @Y("alguien inicia sesión con un correo que no está registrado")
  public void someoneSignsInWithAnUnknownEmail() throws Exception {
    otherResponse = signIn(uniqueEmail(), PASSWORD);
  }

  @Cuando("el usuario intenta registrar una emergencia")
  public void theUserTriesToRegisterAnEmergency() throws Exception {
    response = registerEmergency();
  }

  @Entonces("se concede el acceso con el rol {string}")
  public void accessIsGrantedWithTheRole(String role) throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat(JsonPath.<String>read(body, "$.role")).isEqualTo(role);
    assertThat(JsonPath.<String>read(body, "$.tokenType")).isEqualTo("Bearer");
  }

  @Entonces("el usuario puede registrar una emergencia")
  public void theUserCanRegisterAnEmergency() throws Exception {
    assertThat(registerEmergency().getStatus()).isEqualTo(201);
  }

  @Entonces("los dos accesos se deniegan con el mismo mensaje")
  public void bothAttemptsAreDeniedWithTheSameMessage() throws Exception {
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(otherResponse.getStatus()).isEqualTo(401);
    String detail = JsonPath.read(response.getContentAsString(), "$.detail");
    assertThat(detail).isEqualTo(JsonPath.read(otherResponse.getContentAsString(), "$.detail"));
    assertThat(detail).doesNotContainIgnoringCase("password").doesNotContainIgnoringCase("email");
  }

  @Entonces("el sistema deniega la acción con código {int}")
  public void theActionIsDenied(int status) {
    assertThat(response.getStatus()).isEqualTo(status);
  }

  private MockHttpServletResponse registerEmergency() throws Exception {
    return send(
        post("/api/v1/emergencies"),
        userToken,
        """
        {
          "name": "Huaico Chosica 2026",
          "type": "LANDSLIDE",
          "location": {
            "department": "Lima",
            "province": "Lima",
            "district": "Lurigancho-Chosica"
          },
          "startDate": "2026-03-14"
        }
        """);
  }

  private MockHttpServletResponse signIn(String email, String password) throws Exception {
    return send(
        post("/api/v1/auth/sign-in"),
        "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password));
  }

  private static String tokenOf(MockHttpServletResponse signIn) throws Exception {
    assertThat(signIn.getStatus()).isEqualTo(200);
    return JsonPath.read(signIn.getContentAsString(), "$.token");
  }

  private MockHttpServletResponse send(MockHttpServletRequestBuilder request, String body)
      throws Exception {
    return mockMvc
        .perform(request.with(anonymous()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse send(
      MockHttpServletRequestBuilder request, String token, String body) throws Exception {
    return send(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token), body);
  }

  private static String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@auxia.pe";
  }
}
