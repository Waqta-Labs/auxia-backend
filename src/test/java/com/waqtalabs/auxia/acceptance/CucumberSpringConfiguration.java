package com.waqtalabs.auxia.acceptance;

import com.waqtalabs.auxia.DefaultAuthorityUserConfiguration;
import com.waqtalabs.auxia.FakeAiServiceConfiguration;
import com.waqtalabs.auxia.FakeBlockchainConfiguration;
import com.waqtalabs.auxia.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * The scenarios call the real API, backed by the PostgreSQL and Azurite test containers. The AI
 * Service is the predictable FakeAiService, so each scenario decides what the NLP identifies, and
 * the smart contract is the FakeBlockchainLedger.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  FakeAiServiceConfiguration.class,
  FakeBlockchainConfiguration.class,
  DefaultAuthorityUserConfiguration.class,
  EmergencyScenario.class
})
class CucumberSpringConfiguration {}
