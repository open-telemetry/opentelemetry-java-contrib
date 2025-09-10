/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.CMQXC;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquireChannelCmdCollectorTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  InquireChannelCmdCollector classUnderTest;

  MetricsCollectorContext context;
  Meter meter;
  @Mock PCFMessageAgent pcfMessageAgent;

  @BeforeEach
  public void setup() throws Exception {
    ConfigWrapper config = ConfigWrapper.parse("src/test/resources/conf/config.yml");
    ObjectMapper mapper = new ObjectMapper();
    QueueManager queueManager =
        mapper.convertValue(config.getQueueManagers().get(0), QueueManager.class);
    meter = otelTesting.getOpenTelemetry().getMeter("opentelemetry.io/mq");
    context =
        new MetricsCollectorContext(queueManager, pcfMessageAgent, null, new MetricsConfig(config));
  }

  @Test
  void testProcessPCFRequestAndPublishQMetricsForInquireQStatusCmd() throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class)))
        .thenReturn(createPCFResponseForInquireChannelCmd());
    classUnderTest = new InquireChannelCmdCollector(meter);
    classUnderTest.accept(context);
    List<String> metricsList =
        new ArrayList<>(
            Arrays.asList(
                "ibm.mq.message.retry.count",
                "ibm.mq.message.received.count",
                "ibm.mq.message.sent.count"));
    for (MetricData metric : otelTesting.getMetrics()) {
      if (metricsList.remove(metric.getName())) {
        if (metric.getName().equals("ibm.mq.message.retry.count")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(22);
        }
        if (metric.getName().equals("ibm.mq.message.received.count")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(42);
        }
        if (metric.getName().equals("ibm.mq.message.sent.count")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(64);
        }
      }
    }
    assertThat(metricsList).isEmpty();
  }

  private static PCFMessage[] createPCFResponseForInquireChannelCmd() {
    PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL, 1, true);
    response1.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "my.channel");
    response1.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, CMQXC.MQCHT_SVRCONN);
    response1.addParameter(CMQCFC.MQIACH_MR_COUNT, 22);
    response1.addParameter(CMQCFC.MQIACH_MSGS_RECEIVED, 42);
    response1.addParameter(CMQCFC.MQIACH_MSGS_SENT, 64);
    response1.addParameter(CMQCFC.MQIACH_MAX_INSTANCES, 3);
    response1.addParameter(CMQCFC.MQIACH_MAX_INSTS_PER_CLIENT, 3);

    return new PCFMessage[] {response1};
  }
}
