package com.waqtalabs.auxia.traceability.infrastructure.storage;

import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client of the evidence container. Building it does not connect, so the application starts even
 * if the storage is not reachable yet.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EvidenceStorageProperties.class)
class EvidenceStorageConfiguration {

  @Bean
  BlobContainerClient evidenceContainer(EvidenceStorageProperties properties) {
    return new BlobServiceClientBuilder()
        .connectionString(properties.connectionString())
        .httpClient(new JdkHttpClientBuilder().build())
        .buildClient()
        .getBlobContainerClient(properties.container());
  }
}
