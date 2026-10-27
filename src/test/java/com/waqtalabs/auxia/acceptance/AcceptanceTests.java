package com.waqtalabs.auxia.acceptance;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the {@code .feature} files of {@code src/test/resources/features}, written from the
 * Gherkin criteria of the user and technical stories (chapter III of the report).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.waqtalabs.auxia.acceptance")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class AcceptanceTests {}
