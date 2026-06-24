/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka;

import static io.opentelemetry.contrib.kafka.TestUtil.makeBasicSpan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PooledSpanDataSerializerTest {
  private PooledSpanDataSerializer testSubject;

  @AfterEach
  void tearDown() {
    if (testSubject != null) {
      testSubject.close();
    }
  }

  @Test
  void serialize() {
    testSubject = new PooledSpanDataSerializer();
    SpanData span1 = makeBasicSpan("span-1");
    SpanData span2 = makeBasicSpan("span-2");
    ImmutableList<SpanData> spans = ImmutableList.of(span1, span2);

    byte[] actual = testSubject.serialize("test-topic", spans);

    assertThat(actual).isNotNull();
    assertThat(actual).isNotEmpty();
  }

  @Test
  void serializeEmptyData() {
    testSubject = new PooledSpanDataSerializer();
    byte[] actual = testSubject.serialize("test-topic", Collections.emptySet());

    assertThat(actual).isEmpty();
  }

  @Test
  void serializeNullDataThrowsException() {
    testSubject = new PooledSpanDataSerializer();
    assertThatThrownBy(() -> testSubject.serialize("test-topic", null))
        .isInstanceOf(SerializationException.class)
        .hasMessage("Cannot serialize null");
  }

  @Test
  void serializeMultipleTimesReusesMarshaler() {
    testSubject = new PooledSpanDataSerializer();
    SpanData span1 = makeBasicSpan("span-1");
    SpanData span2 = makeBasicSpan("span-2");
    ImmutableList<SpanData> spans = ImmutableList.of(span1, span2);

    // Serialize multiple times to verify object pooling works
    byte[] result1 = testSubject.serialize("test-topic", spans);
    byte[] result2 = testSubject.serialize("test-topic", spans);
    byte[] result3 = testSubject.serialize("test-topic", spans);

    // All results should be valid and identical (same input)
    assertThat(result1).isNotEmpty();
    assertThat(result2).isEqualTo(result1);
    assertThat(result3).isEqualTo(result1);
  }

  @Test
  void outputMatchesTraditionalSerializer() throws Exception {
    testSubject = new PooledSpanDataSerializer();
    SpanDataSerializer traditionalSerializer = new SpanDataSerializer();

    SpanData span1 = makeBasicSpan("span-1");
    SpanData span2 = makeBasicSpan("span-2");
    ImmutableList<SpanData> spans = ImmutableList.of(span1, span2);

    byte[] pooledResult = testSubject.serialize("test-topic", spans);
    byte[] traditionalResult = traditionalSerializer.serialize("test-topic", spans);

    // Both should be deserializable to the same protobuf message
    ExportTraceServiceRequest pooledRequest = ExportTraceServiceRequest.parseFrom(pooledResult);
    ExportTraceServiceRequest traditionalRequest =
        ExportTraceServiceRequest.parseFrom(traditionalResult);

    // Verify structure
    assertThat(pooledRequest.getResourceSpansList())
        .hasSize(traditionalRequest.getResourceSpansList().size());
    assertThat(pooledRequest.getResourceSpans(0).getScopeSpans(0).getSpans(0).getName())
        .isEqualTo("span-1");
    assertThat(pooledRequest.getResourceSpans(0).getScopeSpans(0).getSpans(1).getName())
        .isEqualTo("span-2");
  }

  @Test
  void concurrentSerializationIsThreadSafe() throws Exception {
    testSubject = new PooledSpanDataSerializer();
    int numThreads = 10;
    int iterationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numThreads);
    List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < numThreads; i++) {
      int threadId = i;
      var unused =
          executor.submit(
              () -> {
                try {
                  startLatch.await(); // Wait for all threads to be ready
                  for (int j = 0; j < iterationsPerThread; j++) {
                    SpanData span = makeBasicSpan("thread-" + threadId + "-span-" + j);
                    ImmutableList<SpanData> spans = ImmutableList.of(span);
                    byte[] result = testSubject.serialize("test-topic", spans);

                    // Verify result is valid
                    assertThat(result).isNotEmpty();
                    ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(result);
                    assertThat(request.getResourceSpans(0).getScopeSpans(0).getSpans(0).getName())
                        .isEqualTo("thread-" + threadId + "-span-" + j);
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  errors.add(e);
                } catch (Throwable e) {
                  errors.add(e);
                } finally {
                  doneLatch.countDown();
                }
              });
    }

    startLatch.countDown(); // Start all threads
    assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
    executor.shutdown();

    // Verify no errors occurred
    if (!errors.isEmpty()) {
      throw new AssertionError("Concurrent serialization failed with errors: " + errors);
    }
  }

  @Test
  void closeReleasesResources() {
    testSubject = new PooledSpanDataSerializer();
    SpanData span = makeBasicSpan("span-1");
    ImmutableList<SpanData> spans = ImmutableList.of(span);

    // Use the serializer
    testSubject.serialize("test-topic", spans);

    // Close should not throw
    testSubject.close();

    // Can still use after close (just won't have pooled resources)
    byte[] result = testSubject.serialize("test-topic", spans);
    assertThat(result).isNotEmpty();
  }
}
