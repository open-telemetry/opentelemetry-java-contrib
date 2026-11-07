/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.opentelemetry;

import io.opentelemetry.ibm.mq.WmqMonitor;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricProducer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Command-line entry point that collects IBM MQ metrics using the given configuration file. */
@SuppressWarnings("SystemOut")
public final class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private Main() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Usage: Main <config-file>");
      System.exit(1);
    }

    try {
      Class.forName("com.ibm.mq.headers.MQDataException");
    } catch (ClassNotFoundException e) {
      System.err.println("IBM MQ jar is missing from classpath.");
      System.exit(1);
    }

    String configFile = args[0];

    ConfigWrapper config = ConfigWrapper.parse(configFile);

    Config.configureSecurity(config);
    Config.setUpSslConnection(config.getSslConnection());
    WmqMonitor monitor = new WmqMonitor(config);
    run(config, monitor);
  }

  @VisibleForTesting
  public static void run(ConfigWrapper config, WmqMonitor monitor) {

    AutoConfiguredOpenTelemetrySdk.builder()
        .addMeterProviderCustomizer((builder, configProps) -> {
          builder.setResource(Resource.empty());
          for (MetricProducer mp : monitor.getMetricProducersByQueueManagers().values()) {
            builder.registerMetricProducer(mp);
          }
          return builder;
        })
        .build();

    int numQueueManagers = monitor.getMetricProducersByQueueManagers().size();

    long skew = 0;
    long periodNanos = TimeUnit.SECONDS.toNanos(config.getTaskDelaySeconds());
    long step = periodNanos / numQueueManagers;

    int threadsPerQueueManager = (config.getNumberOfThreads() / numQueueManagers) - 1;

    if (threadsPerQueueManager < 2) {
      logger.warn(
          "Collection requires at least 2 threads per queue manager ({} provisioned), {} threads configured. Bumping to {} threads.",
          numQueueManagers,
          config.getNumberOfThreads(),
          numQueueManagers * 2);
      threadsPerQueueManager = 2;
    }

    Thread.UncaughtExceptionHandler handler =
        (t, e) -> logger.error("Unhandled exception in thread pool", e);

    ScheduledExecutorService queueManagersThreadPool =
        Executors.newScheduledThreadPool(
            numQueueManagers,
            r -> {
              Thread thread = new Thread(r);
              thread.setUncaughtExceptionHandler(handler);
              return thread;
            });

    Integer collectionTimeout = config.getCollectionTimeoutSeconds();

    for (Map.Entry<QueueManager, MetricProducer> entry :
        monitor.getMetricProducersByQueueManagers().entrySet()) {

      logger.debug("Initializing thread pool with {} threads", threadsPerQueueManager);
      ExecutorService service =
          Executors.newFixedThreadPool(
              threadsPerQueueManager,
              r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler(handler);
                return thread;
              });
      ScheduledFuture<?> unused =
          queueManagersThreadPool.scheduleAtFixedRate(
              () -> {
                monitor.run(entry.getKey(), entry.getValue(), service);
                if (collectionTimeout != null) {
                  try {
                    service.awaitTermination(collectionTimeout, TimeUnit.SECONDS);
                  } catch (InterruptedException e) {
                    logger.error(
                        "Error while the thread {} is waiting ",
                        Thread.currentThread().getName(),
                        e);
                  }
                }
              },
              TimeUnit.SECONDS.toNanos(config.getTaskInitialDelaySeconds()) + skew,
              periodNanos,
              TimeUnit.NANOSECONDS);
      skew += step;
    }
  }
}
