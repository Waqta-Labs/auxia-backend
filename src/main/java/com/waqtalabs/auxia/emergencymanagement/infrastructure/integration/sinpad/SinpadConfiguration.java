package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.sinpad;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Connection settings of SINPAD. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SinpadProperties.class)
class SinpadConfiguration {}
