package io.opentelemetry.contrib.messaging.wrappers.kafka;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapper;
import io.opentelemetry.contrib.messaging.wrappers.kafka.internal.AutoConfiguredDataCapture;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaConsumerAttributesExtractor;
import io.opentelemetry.contrib.messaging.wrappers.kafka.semconv.KafkaProcessRequest;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingConsumerInterceptor;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.api.AbstractAssert;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil.orderByRootSpanName;
import static io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil.waitForTraces;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_CONSUMER_GROUP_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_PARTITION_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_KAFKA_OFFSET;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_BODY_SIZE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static org.awaitility.Awaitility.await;

public class KafkaClientTest extends KafkaClientBaseTest {

  static final String greeting = "Hello Kafka!";

  static final String clientId = "test-consumer-1";

  static final String groupId = "test";

  @Override
  public Map<String, Object> producerProps() {
    Map<String, Object> props = super.producerProps();
    props.put(
        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
    return props;
  }

  @Override
  public Map<String, Object> consumerProps() {
    Map<String, Object> props = super.consumerProps();
    props.put(
        ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    return props;
  }

  @Test
  void testInterceptors() throws InterruptedException {
    OpenTelemetry otel = GlobalOpenTelemetry.get();
    Tracer tracer = otel.getTracer("test-tracer", "1.0.0");
    MessagingProcessWrapper<KafkaProcessRequest> wrapper = KafkaHelper.processWrapperBuilder()
        .openTelemetry(otel)
        .addAttributesExtractor(KafkaConsumerAttributesExtractor.create())
        .build();

    sendWithParent(tracer);

    awaitUntilConsumerIsReady();

    consumeWithChild(tracer, wrapper);

    assertTraces();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public void sendWithParent(Tracer tracer) {
    Span parent = tracer.spanBuilder("parent").startSpan();
    try (Scope scope = parent.makeCurrent()) {
      producer.send(new ProducerRecord<>(SHARED_TOPIC, greeting),
          (meta, ex) -> {
            if (ex == null) {
              tracer.spanBuilder("producer callback").startSpan().end();
            } else {
              tracer.spanBuilder("producer exception: " + ex).startSpan().end();
            }
          });
    }
    parent.end();
  }

  public void consumeWithChild(Tracer tracer, MessagingProcessWrapper<KafkaProcessRequest> wrapper) {
    // check that the message was received
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5));
    assertThat(records.count()).isEqualTo(1);
    ConsumerRecord<?, ?> record = records.iterator().next();
    assertThat(record.value()).isEqualTo(greeting);
    assertThat(record.key()).isNull();

    wrapper.doProcess(KafkaProcessRequest.of(record, groupId, clientId), () -> {
      tracer.spanBuilder("process child").startSpan().end();
    });
  }

  /**
   * Copied from <a href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/testing-common>testing-common</a>.
   * */
  @SuppressWarnings("deprecation") // using deprecated semconv
  public void assertTraces() {
    waitAndAssertTraces(
        orderByRootSpanName("parent", "producer callback"),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    // No need to verify the attribute here because it is generated by instrumentation library.
                    span.hasName(SHARED_TOPIC + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0)),
                span ->
                    span.hasName("process " + SHARED_TOPIC)
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            equalTo(MESSAGING_SYSTEM, "kafka"),
                            equalTo(MESSAGING_DESTINATION_NAME, SHARED_TOPIC),
                            equalTo(
                                MESSAGING_MESSAGE_BODY_SIZE,
                                greeting.getBytes(StandardCharsets.UTF_8).length),
                            satisfies(
                                MESSAGING_DESTINATION_PARTITION_ID,
                                org.assertj.core.api.AbstractStringAssert::isNotEmpty),
                            satisfies(
                                MESSAGING_CLIENT_ID,
                                stringAssert -> stringAssert.isEqualTo("test-consumer-1")),
                            satisfies(
                                MESSAGING_KAFKA_OFFSET,
                                AbstractAssert::isNotNull),
                            equalTo(MESSAGING_CONSUMER_GROUP_NAME, "test"),
                            equalTo(MESSAGING_OPERATION, "process")),
                span ->
                    span.hasName("process child")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))),
        // ideally we'd want producer callback to be part of the main trace, we just aren't able to
        // instrument that
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("producer callback").hasKind(SpanKind.INTERNAL).hasNoParent()));
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static void waitAndAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      Consumer<TraceAssert>... assertions) {
    List<Consumer<TraceAssert>> assertionsList = new ArrayList<>(Arrays.asList(assertions));
    try {
      await()
          .untilAsserted(() -> doAssertTraces(traceComparator, AutoConfiguredDataCapture::getSpans, assertionsList));
    } catch (Throwable t) {
      // awaitility is doing a jmx call that is not implemented in GraalVM:
      // call:
      // https://github.com/awaitility/awaitility/blob/fbe16add874b4260dd240108304d5c0be84eabc8/awaitility/src/main/java/org/awaitility/core/ConditionAwaiter.java#L157
      // see https://github.com/oracle/graal/issues/6101 (spring boot graal native image)
      if (t.getClass().getName().equals("com.oracle.svm.core.jdk.UnsupportedFeatureError")
          || t instanceof ConditionTimeoutException) {
        // Don't throw this failure since the stack is the awaitility thread, causing confusion.
        // Instead, just assert one more time on the test thread, which will fail with a better
        // stack trace.
        // TODO(anuraaga): There is probably a better way to do this.
        doAssertTraces(traceComparator, AutoConfiguredDataCapture::getSpans, assertionsList);
      } else {
        throw t;
      }
    }
  }

  private static void doAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      Supplier<List<SpanData>> supplier,
      List<Consumer<TraceAssert>> assertionsList) {
    try {
      List<List<SpanData>> traces = waitForTraces(supplier, assertionsList.size());
      TelemetryDataUtil.assertScopeVersion(traces);
      if (traceComparator != null) {
        traces.sort(traceComparator);
      }
      TracesAssert.assertThat(traces).hasTracesSatisfyingExactly(assertionsList);
    } catch (InterruptedException | TimeoutException e) {
      throw new AssertionError("Error waiting for " + assertionsList.size() + " traces", e);
    }
  }
}
