package com.waqtalabs.auxia.shared.infrastructure.aiservice;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** HTTP client of the AI Service, shared by the gateways of every bounded context. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceConfiguration {

  public static final String API_KEY_HEADER = "X-API-Key";

  @Bean
  public RestClient aiServiceRestClient(AiServiceProperties properties) {
    var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader(API_KEY_HEADER, properties.apiKey())
        .requestFactory(requestFactory)
        .build();
  }
}
