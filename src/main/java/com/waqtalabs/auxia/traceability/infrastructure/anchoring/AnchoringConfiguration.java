package com.waqtalabs.auxia.traceability.infrastructure.anchoring;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Where the hashes are anchored: one background thread, so the transactions of the wallet go out
 * one after another, and a periodic retry of the pending ones. With {@code
 * auxia.traceability.anchoring.asynchronous=false} the anchoring runs in the thread that
 * registers the delivery, which the tests use to check the result in the same request.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
class AnchoringConfiguration {

  @Bean
  Executor anchoringExecutor(
      @Value("${auxia.traceability.anchoring.asynchronous}") boolean asynchronous) {
    if (!asynchronous) {
      return new SyncTaskExecutor();
    }
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setThreadNamePrefix("anchoring-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();
    return executor;
  }
}
