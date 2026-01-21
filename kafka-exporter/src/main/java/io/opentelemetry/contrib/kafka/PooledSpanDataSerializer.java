/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import io.opentelemetry.exporter.internal.otlp.traces.LowAllocationTraceRequestMarshaler;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object pool optimized Span data serializer.
 *
 * <p>This serializer reduces memory allocations by reusing {@link
 * LowAllocationTraceRequestMarshaler} instances and {@link ByteArrayOutputStream} buffers. It is
 * thread-safe and supports concurrent serialization calls from Kafka.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Enable via MemoryMode configuration
 * KafkaSpanExporter exporter = KafkaSpanExporter.newBuilder()
 *     .setProducer(
 *         KafkaSpanExporterBuilder.ProducerBuilder.newInstance()
 *             .setMemoryMode(MemoryMode.REUSABLE_DATA)
 *             .build())
 *     .build();
 * }</pre>
 */
public final class PooledSpanDataSerializer implements Serializer<Collection<SpanData>> {

  private static final Logger logger = LoggerFactory.getLogger(PooledSpanDataSerializer.class);

  // Object pool: thread-safe lock-free queue
  private final Deque<LowAllocationTraceRequestMarshaler> marshalerPool =
      new ConcurrentLinkedDeque<>();

  // Thread-local ByteArrayOutputStream to avoid contention
  private final ThreadLocal<ByteArrayOutputStream> outputStreamHolder =
      ThreadLocal.withInitial(() -> new ByteArrayOutputStream(4096));

  // Maximum pool size to prevent unbounded growth
  private static final int MAX_POOL_SIZE = 32;

  @Override
  public byte[] serialize(String topic, Collection<SpanData> data) {
    if (Objects.isNull(data)) {
      throw new SerializationException("Cannot serialize null");
    }

    if (data.isEmpty()) {
      return new byte[0];
    }

    // 1. Acquire marshaler from pool
    LowAllocationTraceRequestMarshaler marshaler = marshalerPool.poll();
    if (marshaler == null) {
      // Pool is empty, create new instance
      marshaler = new LowAllocationTraceRequestMarshaler();
    }

    // 2. Get thread-local ByteArrayOutputStream
    ByteArrayOutputStream baos = outputStreamHolder.get();
    baos.reset();

    try {
      // 3. Initialize and serialize (Initialize-Use pattern)
      marshaler.initialize(data);
      marshaler.writeBinaryTo(baos);

      // 4. Return result
      return baos.toByteArray();

    } catch (IOException e) {
      throw new SerializationException("Failed to serialize span data", e);
    } finally {
      // 5. Reset and return marshaler to pool (Reset-Return pattern)
      marshaler.reset();
      returnToPool(marshaler);
    }
  }

  /**
   * Returns marshaler to pool, with size limit to prevent memory leaks.
   *
   * @param marshaler the marshaler to return
   */
  private void returnToPool(LowAllocationTraceRequestMarshaler marshaler) {
    if (marshalerPool.size() < MAX_POOL_SIZE) {
      marshalerPool.offer(marshaler);
    } else {
      // Pool is full, discard the instance and let GC reclaim it
      // This is a defensive strategy to prevent unbounded growth in exceptional cases
      if (logger.isDebugEnabled()) {
        logger.debug("Marshaler pool is full, discarding instance");
      }
    }
  }

  @Override
  public void close() {
    // Clean up resources
    marshalerPool.clear();
    outputStreamHolder.remove();
  }
}
