package com.waqtalabs.auxia.traceability.infrastructure.blockchain;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.application.ports.BlockchainLedgerPort;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;

/**
 * {@code DeliveryRegistry} through Web3j (Conformist). The delivery is its UUID as 16 bytes and
 * the hash its 32 bytes, as the contract expects.
 *
 * <p>Transactions are signed with the wallet that owns the contract. They are sent one at a time,
 * so two anchorings never take the same nonce.
 */
@Component
class Web3jBlockchainLedger implements BlockchainLedgerPort {

  static final String SERVICE_NAME = "Blockchain";

  private static final Logger LOGGER = LoggerFactory.getLogger(Web3jBlockchainLedger.class);
  private static final String SUCCESS = "0x1";
  /** Room over the gas estimate, in percent. */
  private static final BigInteger GAS_MARGIN = BigInteger.valueOf(120);

  private final Web3j web3j;
  private final BlockchainProperties properties;
  private final Credentials credentials;
  private final RawTransactionManager transactions;

  Web3jBlockchainLedger(Web3j web3j, BlockchainProperties properties) {
    this.web3j = web3j;
    this.properties = properties;
    this.credentials = Credentials.create(properties.privateKey());
    this.transactions = new RawTransactionManager(web3j, credentials, properties.chainId());
  }

  @Override
  public synchronized String registerDelivery(DeliveryId deliveryId, HashValue hash) {
    String data =
        FunctionEncoder.encode(
            new Function(
                "registerDelivery",
                List.of(new Bytes16(bytesOf(deliveryId.value())), new Bytes32(hash.bytes())),
                List.of()));
    try {
      BigInteger gasPrice = check(web3j.ethGasPrice().send()).getGasPrice();
      BigInteger gasLimit =
          check(
                  web3j
                      .ethEstimateGas(
                          Transaction.createFunctionCallTransaction(
                              credentials.getAddress(),
                              null,
                              gasPrice,
                              null,
                              properties.contractAddress(),
                              data))
                      .send())
              .getAmountUsed()
              .multiply(GAS_MARGIN)
              .divide(BigInteger.valueOf(100));
      return check(
              transactions.sendTransaction(
                  gasPrice, gasLimit, properties.contractAddress(), data, BigInteger.ZERO))
          .getTransactionHash();
    } catch (IOException ex) {
      throw unavailable(ex);
    }
  }

  @Override
  public TransactionOutcome confirmation(String transactionHash) {
    long deadline = System.nanoTime() + properties.confirmationTimeout().toNanos();
    try {
      while (true) {
        Optional<TransactionReceipt> receipt =
            check(web3j.ethGetTransactionReceipt(transactionHash).send()).getTransactionReceipt();
        if (receipt.isPresent()) {
          return SUCCESS.equals(receipt.get().getStatus())
              ? TransactionOutcome.CONFIRMED
              : TransactionOutcome.REVERTED;
        }
        if (System.nanoTime() >= deadline) {
          return TransactionOutcome.PENDING;
        }
        Thread.sleep(properties.pollInterval());
      }
    } catch (IOException ex) {
      throw unavailable(ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return TransactionOutcome.PENDING;
    }
  }

  @Override
  public Optional<HashValue> deliveryHash(DeliveryId deliveryId) {
    Function function =
        new Function(
            "getDeliveryHash",
            List.of(new Bytes16(bytesOf(deliveryId.value()))),
            List.of(new TypeReference<Bytes32>() {}));
    try {
      String value =
          check(
                  web3j
                      .ethCall(
                          Transaction.createEthCallTransaction(
                              credentials.getAddress(),
                              properties.contractAddress(),
                              FunctionEncoder.encode(function)),
                          DefaultBlockParameterName.LATEST)
                      .send())
              .getValue();
      @SuppressWarnings("rawtypes")
      List<Type> decoded = FunctionReturnDecoder.decode(value, function.getOutputParameters());
      if (decoded.isEmpty()) {
        throw new ExternalServiceUnavailableException(SERVICE_NAME, null);
      }
      byte[] hash = ((Bytes32) decoded.getFirst()).getValue();
      return Arrays.equals(hash, new byte[32]) ? Optional.empty() : Optional.of(HashValue.of(hash));
    } catch (IOException ex) {
      throw unavailable(ex);
    }
  }

  private static byte[] bytesOf(UUID uuid) {
    return ByteBuffer.allocate(16)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits())
        .array();
  }

  /** A JSON-RPC error, such as a call that the contract reverts, makes the network unavailable. */
  private static <T extends Response<?>> T check(T response) {
    if (response.hasError()) {
      LOGGER.warn("JSON-RPC error: {}", response.getError().getMessage());
      throw new ExternalServiceUnavailableException(SERVICE_NAME, null);
    }
    return response;
  }

  private static ExternalServiceUnavailableException unavailable(IOException ex) {
    LOGGER.warn("The network did not answer: {}", ex.getMessage());
    return new ExternalServiceUnavailableException(SERVICE_NAME, ex);
  }
}
