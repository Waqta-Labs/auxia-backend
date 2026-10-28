package com.waqtalabs.auxia.traceability.infrastructure.blockchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.application.ports.BlockchainLedgerPort.TransactionOutcome;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

/**
 * The adapter against the real {@code DeliveryRegistry}, deployed on a local Anvil chain. The
 * bytecode in {@code blockchain/DeliveryRegistry.bin} is the one compiled by
 * auxia-smart-contracts and deployed on Sepolia. The keys are the first two public test accounts
 * of Anvil, which hold nothing real.
 */
@Testcontainers
class Web3jBlockchainLedgerTests {

  private static final DockerImageName ANVIL =
      DockerImageName.parse("ghcr.io/foundry-rs/foundry:v1.2.3");
  private static final long CHAIN_ID = 31337;
  private static final String OWNER_KEY =
      "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
  private static final String OTHER_KEY =
      "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d";

  @Container
  private static final GenericContainer<?> CHAIN =
      new GenericContainer<>(ANVIL)
          // The image runs its command through "sh -c"; anvil is called directly instead.
          .withCreateContainerCmdModifier(command -> command.withEntrypoint("anvil"))
          .withCommand("--host", "0.0.0.0", "--chain-id", String.valueOf(CHAIN_ID))
          .withExposedPorts(8545)
          // Any HTTP answer, even 400 to a GET, shows that the port of the chain is reachable.
          .waitingFor(Wait.forHttp("/").forStatusCode(400));

  private static Web3j web3j;
  private static String contract;

  @BeforeAll
  static void deployTheContract() throws Exception {
    web3j = Web3j.build(new HttpService(rpcUrl().toString()));
    var owner = new RawTransactionManager(web3j, Credentials.create(OWNER_KEY), CHAIN_ID);
    String transaction =
        owner
            .sendTransaction(
                web3j.ethGasPrice().send().getGasPrice(),
                BigInteger.valueOf(1_000_000),
                null,
                bytecode(),
                BigInteger.ZERO)
            .getTransactionHash();
    TransactionReceipt receipt =
        new PollingTransactionReceiptProcessor(web3j, 200, 50)
            .waitForTransactionReceipt(transaction);
    contract = receipt.getContractAddress();
  }

  @AfterAll
  static void shutdown() {
    web3j.shutdown();
  }

  @Test
  void hashIsAnchoredAndReadBack() {
    var ledger = ledger(OWNER_KEY);
    var delivery = new DeliveryId(UUID.randomUUID());
    var hash = new HashValue("ab".repeat(32));

    assertThat(ledger.deliveryHash(delivery)).isEmpty();
    String transaction = ledger.registerDelivery(delivery, hash);

    assertThat(transaction).matches("0x[0-9a-f]{64}");
    assertThat(ledger.confirmation(transaction)).isEqualTo(TransactionOutcome.CONFIRMED);
    assertThat(ledger.deliveryHash(delivery)).contains(hash);
  }

  @Test
  void anchoredHashCannotBeReplaced() {
    var ledger = ledger(OWNER_KEY);
    var delivery = new DeliveryId(UUID.randomUUID());
    var hash = new HashValue("cd".repeat(32));
    ledger.confirmation(ledger.registerDelivery(delivery, hash));
    var other = new HashValue("ef".repeat(32));

    assertThatThrownBy(() -> ledger.registerDelivery(delivery, other))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    assertThat(ledger.deliveryHash(delivery)).contains(hash);
  }

  @Test
  void onlyTheOwnerOfTheContractAnchors() {
    var ledger = ledger(OTHER_KEY);
    var delivery = new DeliveryId(UUID.randomUUID());
    var hash = new HashValue("12".repeat(32));

    assertThatThrownBy(() -> ledger.registerDelivery(delivery, hash))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    assertThat(ledger.deliveryHash(delivery)).isEmpty();
  }

  @Test
  void unknownTransactionStaysPendingUntilTheWaitEnds() {
    var ledger = ledger(OWNER_KEY);

    assertThat(ledger.confirmation("0x" + "0".repeat(63) + "1"))
        .isEqualTo(TransactionOutcome.PENDING);
  }

  @Test
  void unreachableNetworkIsUnavailable() {
    var properties =
        new BlockchainProperties(
            URI.create("http://localhost:1"),
            OWNER_KEY,
            contract,
            CHAIN_ID,
            Duration.ofMillis(300),
            Duration.ofMillis(100));
    var unreachable =
        new Web3jBlockchainLedger(Web3j.build(new HttpService("http://localhost:1")), properties);
    var delivery = new DeliveryId(UUID.randomUUID());

    assertThatThrownBy(() -> unreachable.deliveryHash(delivery))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  @Test
  void propertiesNeverShowThePrivateKey() {
    assertThat(ledgerProperties(OWNER_KEY).toString())
        .doesNotContain(OWNER_KEY.substring(2))
        .contains(contract);
  }

  private static Web3jBlockchainLedger ledger(String key) {
    return new Web3jBlockchainLedger(web3j, ledgerProperties(key));
  }

  private static BlockchainProperties ledgerProperties(String key) {
    return new BlockchainProperties(
        rpcUrl(), key, contract, CHAIN_ID, Duration.ofMillis(600), Duration.ofMillis(100));
  }

  private static URI rpcUrl() {
    return URI.create("http://" + CHAIN.getHost() + ":" + CHAIN.getMappedPort(8545));
  }

  private static String bytecode() throws IOException {
    try (InputStream input =
        Web3jBlockchainLedgerTests.class.getResourceAsStream("/blockchain/DeliveryRegistry.bin")) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8).strip();
    }
  }
}
