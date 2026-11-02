package com.waqtalabs.auxia.traceability.infrastructure.blockchain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Network and contract where the hashes are anchored, under {@code auxia.blockchain}.
 *
 * @param rpcUrl JSON-RPC endpoint of the network (Alchemy or Infura)
 * @param privateKey key of the wallet that owns the contract: only it can register a delivery
 * @param contractAddress address of {@code DeliveryRegistry}
 * @param chainId network of the transactions (11155111 is Sepolia)
 * @param confirmationTimeout how long a transaction is awaited before trying later
 * @param pollInterval how often its receipt is asked for while it is awaited
 */
@Validated
@ConfigurationProperties("auxia.blockchain")
public record BlockchainProperties(
    @NotNull URI rpcUrl,
    @NotBlank @Pattern(regexp = "(0x)?[0-9a-fA-F]{64}") String privateKey,
    @NotBlank @Pattern(regexp = "0x[0-9a-fA-F]{40}") String contractAddress,
    @Positive long chainId,
    @DefaultValue("90s") Duration confirmationTimeout,
    @DefaultValue("3s") Duration pollInterval) {

  /** Never shows the private key, for example in a log of the configuration. */
  @Override
  public String toString() {
    return "BlockchainProperties[rpcUrl=%s, contractAddress=%s, chainId=%d]"
        .formatted(rpcUrl.getHost(), contractAddress, chainId);
  }
}
