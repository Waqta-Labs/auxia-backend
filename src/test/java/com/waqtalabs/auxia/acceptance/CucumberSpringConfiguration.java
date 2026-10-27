package com.waqtalabs.auxia.acceptance;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/** The scenarios call the real API, backed by the PostgreSQL test container. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CucumberSpringConfiguration {}
