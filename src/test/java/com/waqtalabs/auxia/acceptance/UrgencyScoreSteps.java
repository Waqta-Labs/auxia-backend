package com.waqtalabs.auxia.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.waqtalabs.auxia.emergencymanagement.application.FakeAiService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Steps of US-02, US-04, US-05, US-06 and TS-02. The AI Service is {@link FakeAiService}: what the
 * NLP identifies is set by each scenario, and its score is {@code 40 × damage + 60 ×
 * peopleAffected / population}.
 */
public class UrgencyScoreSteps {

  private final EmergencyScenario scenario;
  private final FakeAiService aiService;

  public UrgencyScoreSteps(EmergencyScenario scenario, FakeAiService aiService) {
    this.scenario = scenario;
    this.aiService = aiService;
  }

  @Before
  public void resetAiService() {
    aiService.reset();
  }

  @Dado("una emergencia activa {string} con la zona {string} de {int} habitantes")
  public void anActiveEmergencyWithAZoneOfPopulation(
      String emergencyName, String zoneName, int population) throws Exception {
    scenario.registerEmergency(emergencyName);
    scenario.registerZone(zoneName, population, "SEVERE");
  }

  @Dado("una emergencia activa {string} con estas zonas:")
  public void anActiveEmergencyWithTheseZones(String emergencyName, DataTable zones)
      throws Exception {
    scenario.registerEmergency(emergencyName);
    for (Map<String, String> zone : zones.asMaps()) {
      scenario.registerZone(
          zone.get("zona"), Integer.valueOf(zone.get("habitantes")), zone.get("daño"));
    }
  }

  @Dado("que el NLP identifica en el reporte {string} el daño {string}, {int} personas afectadas"
      + " y la necesidad {string}")
  public void theNlpIdentifies(String text, String damageLevel, int peopleAffected, String need) {
    aiService.willStructure(
        text,
        new StructuredFieldData(
            DamageLevel.valueOf(damageLevel),
            null,
            peopleAffected,
            null,
            null,
            null,
            Set.of(Need.valueOf(need)),
            List.of("familiesAffected", "children", "elderlyPeople", "disabledPeople")));
  }

  @Dado("que el NLP no identifica ninguna variable en el reporte {string}")
  public void theNlpIdentifiesNothing(String text) {
    // FakeAiService answers that nothing was identified for any text it does not know.
  }

  @Dado("que un reporte de la zona {string} menciona la necesidad {string}")
  public void aReportOfTheZoneMentionsTheNeed(String zoneName, String need) throws Exception {
    String text = "Se necesita " + need + " en " + zoneName;
    aiService.willStructure(
        text,
        new StructuredFieldData(
            null, null, null, null, null, null, Set.of(Need.valueOf(need)), List.of()));
    scenario.send(
        post("/api/v1/zones/" + scenario.zoneId(zoneName) + "/field-reports"),
        "{\"text\": \"%s\", \"source\": \"AUTHORITY\"}".formatted(text));
    assertThat(scenario.response().getStatus()).isEqualTo(201);
  }

  @Cuando("el sistema calcula el score de urgencia de la zona {string}")
  public void theSystemCalculatesTheScore(String zoneName) throws Exception {
    scenario.send(post(score(scenario.zoneId(zoneName))));
  }

  @Cuando("el sistema intenta calcular el score de urgencia de la zona {string}")
  public void theSystemTriesToCalculateTheScore(String zoneName) throws Exception {
    scenario.send(post(score(scenario.zoneId(zoneName))));
  }

  @Cuando("la autoridad solicita el detalle explicativo del score de la zona {string}")
  public void theAuthorityAsksForTheExplanation(String zoneName) throws Exception {
    scenario.send(get(score(scenario.zoneId(zoneName))));
  }

  @Cuando("se envía una solicitud GET del score de la zona {string}")
  public void aGetOfTheScoreIsSent(String zoneName) throws Exception {
    scenario.send(get(score(scenario.zoneId(zoneName))));
  }

  @Cuando("se envía una solicitud GET del score de una zona inexistente")
  public void aGetOfTheScoreOfAnUnknownZoneIsSent() throws Exception {
    scenario.send(get(score(UUID.randomUUID().toString())));
  }

  @Cuando("la autoridad solicita el listado ordenado de zonas")
  public void theAuthorityAsksForTheRanking() throws Exception {
    scenario.send(get(zones()));
  }

  @Cuando("la autoridad filtra las zonas con un score mínimo de {int}")
  public void theAuthorityFiltersByMinimumScore(int minScore) throws Exception {
    scenario.send(get(zones()).param("minScore", String.valueOf(minScore)));
  }

  @Cuando("la autoridad filtra las zonas con la necesidad {string}")
  public void theAuthorityFiltersByNeed(String need) throws Exception {
    scenario.send(get(zones()).param("need", need));
  }

  @Entonces("la zona {string} tiene un score de urgencia con su fecha de cálculo")
  public void theZoneHasAScoreWithItsDate(String zoneName) throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(200);
    assertThat(scenario.<String>read("$.zoneId")).isEqualTo(scenario.zoneId(zoneName));
    assertThat(scenario.<Boolean>read("$.available")).isTrue();
    assertThat(scenario.<Double>read("$.score")).isBetween(0.0, 100.0);
    assertThat(scenario.<String>read("$.calculatedAt")).isNotBlank();
  }

  @Entonces("la zona {string} se marca como score no disponible")
  public void theZoneIsMarkedAsScoreNotAvailable(String zoneName) throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(422);
    scenario.send(get(score(scenario.zoneId(zoneName))));
    assertThat(scenario.<Boolean>read("$.available")).isFalse();
    assertThat(scenario.<List<String>>read("$.missingVariables")).containsExactly("population");
  }

  @Entonces("se muestran las variables que más influyeron en el score, de mayor a menor")
  public void theTopFeaturesAreShown() throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(200);
    List<String> features = scenario.read("$.topFeatures[*].feature");
    List<Double> contributions = scenario.read("$.topFeatures[*].contribution");
    assertThat(features).isNotEmpty();
    assertThat(contributions).isSortedAccordingTo((first, second) -> second.compareTo(first));
    assertThat(scenario.<String>read("$.modelVersion")).isNotBlank();
  }

  @Entonces("el endpoint responde con código 200, el score calculado y sus variables explicativas")
  public void theEndpointRespondsWithTheScore() throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(200);
    assertThat(scenario.<Double>read("$.score")).isNotNull();
    assertThat(scenario.<List<String>>read("$.topFeatures[*].feature")).isNotEmpty();
  }

  @Entonces("el endpoint responde con código {int}")
  public void theEndpointRespondsWith(int status) {
    assertThat(scenario.response().getStatus()).isEqualTo(status);
  }

  @Entonces("las zonas se presentan en este orden: {string}")
  public void theZonesComeInThisOrder(String names) throws Exception {
    assertThat(rankedZoneNames()).containsExactlyElementsOf(split(names));
  }

  @Entonces("solo se muestran las zonas {string}")
  public void onlyTheseZonesAreShown(String names) throws Exception {
    assertThat(rankedZoneNames()).containsExactlyElementsOf(split(names));
  }

  private List<String> rankedZoneNames() throws Exception {
    assertThat(scenario.response().getStatus()).isEqualTo(200);
    List<String> ids = scenario.read("$[*].id");
    return ids.stream().map(scenario::zoneName).toList();
  }

  private String zones() {
    return "/api/v1/emergencies/" + scenario.emergencyId() + "/zones";
  }

  private static String score(String zoneId) {
    return "/api/v1/zones/" + zoneId + "/score";
  }

  private static List<String> split(String names) {
    return Arrays.stream(names.split(",")).map(String::strip).toList();
  }
}
