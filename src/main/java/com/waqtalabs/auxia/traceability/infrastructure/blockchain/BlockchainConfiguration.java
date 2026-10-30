package com.waqtalabs.auxia.traceability.infrastructure.blockchain;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/** JSON-RPC client of the network. Building it does not connect. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BlockchainProperties.class)
class BlockchainConfiguration {

  @Bean(destroyMethod = "shutdown")
  Web3j web3j(BlockchainProperties properties) {
    return Web3j.build(new HttpService(properties.rpcUrl().toString()));
  }
}
