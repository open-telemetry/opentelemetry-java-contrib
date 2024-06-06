/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class InferredSpansProcessor implements SpanProcessor {

  private static final Logger logger = Logger.getLogger(InferredSpansProcessor.class.getName());

  public static final String TRACER_NAME = "elastic-inferred-spans";

  // Visible for testing
  final SamplingProfiler profiler;

  private Tracer tracer;

  InferredSpansProcessor(
      InferredSpansConfiguration config,
      SpanAnchoredClock clock,
      boolean startScheduledProfiling,
      @Nullable File activationEventsFile,
      @Nullable File jfrFile) {
    profiler = new SamplingProfiler(config, clock, this::getTracer, activationEventsFile, jfrFile);
    if (startScheduledProfiling) {
      profiler.start();
    }
  }

  public static InferredSpansProcessorBuilder builder() {
    return new InferredSpansProcessorBuilder();
  }

  /**
   * @param provider the provider to use. Null means that {@link GlobalOpenTelemetry} will be used
   *     lazily.
   */
  public synchronized void setTracerProvider(TracerProvider provider) {
    //TODO: get version from resource
    tracer = provider.get(TRACER_NAME, "todo");
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
  public CompletableResultCode shutdown() {
    CompletableResultCode result = new CompletableResultCode();
    logger.fine("Stopping Inferred Spans Processor");
    ThreadFactory threadFactory = r -> {
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
              } catch (Exception e) {
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
          setTracerProvider(GlobalOpenTelemetry.get().getTracerProvider());
        }
      }
    }
    return tracer;
  }

}
