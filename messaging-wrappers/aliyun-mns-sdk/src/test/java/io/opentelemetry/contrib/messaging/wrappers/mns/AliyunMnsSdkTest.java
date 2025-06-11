/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_BODY_SIZE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_ENVELOPE_SIZE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.common.ServiceHandlingRequiredException;
import com.aliyun.mns.common.http.ClientConfiguration;
import com.aliyun.mns.model.Message;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapper;
import io.opentelemetry.contrib.messaging.wrappers.mns.broker.SmqMockedBroker;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MnsProcessRequest;
import io.opentelemetry.contrib.messaging.wrappers.testing.AbstractBaseTest;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SuppressWarnings("OtelInternalJavadoc")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    classes = {SmqMockedBroker.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class AliyunMnsSdkTest extends AbstractBaseTest {

  private static final String TEST_ENDPOINT = "http://test.mns.cn-hangzhou.aliyuncs.com";

  private static final String QUEUE = "TEST_QUEUE";

  private static final String MESSAGE_BODY = "Hello OpenTelemetry";

  @LocalServerPort private int testApplicationPort; // port at which the spring app is running

  private MNSClient mnsClient;

  private CloudQueue queue;

  private OpenTelemetry otel;

  private Tracer tracer;

  private MessagingProcessWrapper<MnsProcessRequest> wrapper;

  @BeforeAll
  void setupClass() {
    otel = GlobalOpenTelemetry.get();
    tracer = otel.getTracer("test-tracer", "1.0.0");
    wrapper = MnsHelper.processWrapperBuilder().openTelemetry(otel).build();

    ClientConfiguration configuration = new ClientConfiguration();
    configuration.setProxyHost("127.0.0.1");
    configuration.setProxyPort(testApplicationPort);

    CloudAccount account = new CloudAccount("test-ak", "test-sk", TEST_ENDPOINT, configuration);

    mnsClient = account.getMNSClient();
    queue = mnsClient.getQueueRef(QUEUE);
  }

  @Test
  void testSendAndConsume() throws ServiceHandlingRequiredException {
    sendWithParent();

    consumeWithChild();

    assertTraces();
  }

  public void sendWithParent() {
    // mock a send span
    Span parent = tracer.spanBuilder("publish " + QUEUE).setSpanKind(SpanKind.PRODUCER).startSpan();

    try (Scope scope = parent.makeCurrent()) {
      Message message = new Message(MESSAGE_BODY);
      otel.getPropagators()
          .getTextMapPropagator()
          .inject(Context.current(), message, MnsTextMapSetter.INSTANCE);
      queue.putMessage(message);
    }

    parent.end();
  }

  public void consumeWithChild() throws ServiceHandlingRequiredException {
    // check that the message was received
    Message message = null;
    for (int i = 0; i < 3; i++) {
      message = queue.popMessage(3);
      if (message != null) {
        break;
      }
    }

    assertThat(message).isNotNull();

    wrapper.doProcess(
        MnsProcessRequest.of(message, QUEUE),
        () -> {
          tracer.spanBuilder("process child").startSpan().end();
        });
  }

  /**
   * Copied from <a
   * href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/testing-common>testing-common</a>.
   */
  @SuppressWarnings("deprecation") // using deprecated semconv
  public void assertTraces() {
    waitAndAssertTraces(
        sortByRootSpanName("parent", "producer callback"),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    // No need to verify the attribute here because it is generated by
                    // instrumentation library.
                    span.hasName("publish " + QUEUE).hasKind(SpanKind.PRODUCER).hasNoParent(),
                span ->
                    span.hasName(QUEUE + " process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(MESSAGING_SYSTEM, "smq"),
                            equalTo(MESSAGING_DESTINATION_NAME, QUEUE),
                            equalTo(
                                MESSAGING_MESSAGE_BODY_SIZE,
                                MESSAGE_BODY.getBytes(StandardCharsets.UTF_8).length),
                            equalTo(
                                MESSAGING_MESSAGE_ENVELOPE_SIZE,
                                Base64.encodeBase64(MESSAGE_BODY.getBytes(StandardCharsets.UTF_8))
                                    .length),
                            equalTo(MESSAGING_OPERATION, "process")),
                span ->
                    span.hasName("process child")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(1))));
  }
}
