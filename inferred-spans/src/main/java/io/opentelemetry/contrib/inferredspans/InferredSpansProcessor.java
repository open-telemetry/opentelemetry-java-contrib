/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.inferredspans.internal.InferredSpansConfiguration;
import io.opentelemetry.contrib.inferredspans.internal.SamplingProfiler;
import io.opentelemetry.contrib.inferredspans.internal.SpanAnchoredClock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class InferredSpansProcessor implements SpanProcessor {

  private static final Logger logger = Logger.getLogger(InferredSpansProcessor.class.getName());

  public static final String TRACER_NAME = "inferred-spans";
  public static final String TRACER_VERSION = readInferredSpansVersion();

  // Visible for testing
  final SamplingProfiler profiler;
  private final InferredSpansConfiguration config;

  private Supplier<TracerProvider> tracerProvider = GlobalOpenTelemetry::getTracerProvider;
  @Nullable private volatile Tracer tracer;

  InferredSpansProcessor(
      InferredSpansConfiguration config,
      SpanAnchoredClock clock,
      boolean startScheduledProfiling,
      @Nullable File activationEventsFile,
      @Nullable File jfrFile) {
    this.config = config;
    profiler = new SamplingProfiler(config, clock, this::getTracer, activationEventsFile, jfrFile);
    if (startScheduledProfiling) {
      profiler.start();
    }
  }

  public Duration setProfilerInterval(Duration interval) {
    Duration oldInterval = config.setProfilerInterval(interval);
    profiler.reschedule();
    return oldInterval
  }

  public static InferredSpansProcessorBuilder builder() {
    return new InferredSpansProcessorBuilder();
  }

  /**
   * Allows customization of the TraceProvider to use. If not set, a TraceProvider from {@link
   * GlobalOpenTelemetry} will be used.
   *
   * @param provider the provider to use. Null means that {@link GlobalOpenTelemetry} will be used
   *     lazily.
   */
  public synchronized void setTracerProvider(TracerProvider provider) {
    if (provider == null) {
      this.tracerProvider = GlobalOpenTelemetry::getTracerProvider;
    } else {
      this.tracerProvider = () -> provider;
    }
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    profiler.getClock().onSpanStart(span, parentContext);
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  @SuppressWarnings({"FutureReturnValueIgnored", "InterruptedExceptionSwallowed"})
  public CompletableResultCode shutdown() {
    CompletableResultCode result = new CompletableResultCode();
    logger.fine("Stopping Inferred Spans Processor");
    ThreadFactory threadFactory =
        r -> {
          Thread thread = new Thread(r);
          thread.setDaemon(false);
          thread.setName("otel-inferred-spans-shutdown");
          return thread;
        };
    Executors.newSingleThreadExecutor(threadFactory)
        .submit(
            () -> {
              try {
                profiler.stop();
                result.succeed();
              } catch (Throwable e) {
                logger.log(Level.SEVERE, "Failed to stop Inferred Spans Processor", e);
                result.fail();
              }
            });
    return result;
  }

  private Tracer getTracer() {
    if (tracer == null) {
      synchronized (this) {
        if (tracer == null) {
          tracer = tracerProvider.get().get(TRACER_NAME, TRACER_VERSION);
        }
      }
    }
    return tracer;
  }

  private static String readInferredSpansVersion() {
    try (InputStream is = InferredSpansProcessor.class.getResourceAsStream("version.properties")) {
      Properties properties = new Properties();
      properties.load(is);
      String version = (String) properties.get("contrib.version");
      requireNonNull(version);
      return version;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
