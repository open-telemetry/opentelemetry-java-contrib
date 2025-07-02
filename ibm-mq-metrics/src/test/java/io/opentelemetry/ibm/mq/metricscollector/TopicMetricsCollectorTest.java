/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TopicMetricsCollectorTest {
  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  TopicMetricsCollector classUnderTest;
  QueueManager queueManager;
  ConfigWrapper config;
  @Mock private PCFMessageAgent pcfMessageAgent;

  @BeforeEach
  void setup() throws Exception {
    config = ConfigWrapper.parse("src/test/resources/conf/config.yml");
    ObjectMapper mapper = new ObjectMapper();
    queueManager = mapper.convertValue(config.getQueueManagers().get(0), QueueManager.class);
  }

  @Test
  void testPublishMetrics() throws Exception {
    MetricsCollectorContext context =
        new MetricsCollectorContext(queueManager, pcfMessageAgent, null, new MetricsConfig(config));
    classUnderTest =
        new TopicMetricsCollector(otelTesting.getOpenTelemetry().getMeter("opentelemetry.io/mq"));

    when(pcfMessageAgent.send(any(PCFMessage.class)))
        .thenReturn(createPCFResponseForInquireTopicStatusCmd());

    classUnderTest.accept(context);

    List<String> metricsList =
        new ArrayList<>(Arrays.asList("mq.publish.count", "mq.subscription.count"));

    for (MetricData metric : otelTesting.getMetrics()) {
      if (metricsList.remove(metric.getName())) {
        if (metric.getName().equals("mq.publish.count")) {
          Set<Long> values = new HashSet<>();
          values.add(2L);
          values.add(3L);
          assertThat(
                  metric.getLongGaugeData().getPoints().stream()
                      .map(LongPointData::getValue)
                      .collect(Collectors.toSet()))
              .isEqualTo(values);
        }
        if (metric.getName().equals("mq.subscription.count")) {
          Set<Long> values = new HashSet<>();
          values.add(3L);
          values.add(4L);
          assertThat(
                  metric.getLongGaugeData().getPoints().stream()
                      .map(LongPointData::getValue)
                      .collect(Collectors.toSet()))
              .isEqualTo(values);
        }
      }
    }
    assertThat(metricsList).isEmpty();
  }

  private static PCFMessage[] createPCFResponseForInquireTopicStatusCmd() {
    PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, 1, false);
    response1.addParameter(CMQC.MQCA_TOPIC_STRING, "test");
    response1.addParameter(CMQC.MQIA_PUB_COUNT, 2);
    response1.addParameter(CMQC.MQIA_SUB_COUNT, 3);

    PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, 2, false);
    response2.addParameter(CMQC.MQCA_TOPIC_STRING, "dev");
    response2.addParameter(CMQC.MQIA_PUB_COUNT, 3);
    response2.addParameter(CMQC.MQIA_SUB_COUNT, 4);

    PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, 3, false);
    response3.addParameter(CMQC.MQCA_TOPIC_STRING, "system");
    response3.addParameter(CMQC.MQIA_PUB_COUNT, 5);
    response3.addParameter(CMQC.MQIA_SUB_COUNT, 6);

    return new PCFMessage[] {response1, response2, response3};
  }
}
