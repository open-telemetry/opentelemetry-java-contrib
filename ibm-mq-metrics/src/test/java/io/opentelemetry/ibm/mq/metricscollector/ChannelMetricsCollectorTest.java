/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static com.ibm.mq.constants.CMQC.MQRC_SELECTOR_ERROR;
import static com.ibm.mq.constants.CMQCFC.MQRCCF_CHL_STATUS_NOT_FOUND;
import static io.opentelemetry.ibm.mq.metricscollector.MetricAssert.assertThatMetric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChannelMetricsCollectorTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  ChannelMetricsCollector classUnderTest;
  QueueManager queueManager;
  MetricsCollectorContext context;
  Meter meter;
  @Mock PCFMessageAgent pcfMessageAgent;

  @BeforeEach
  void setup() throws Exception {
    ConfigWrapper config = ConfigWrapper.parse("src/test/resources/conf/config.yml");
    ObjectMapper mapper = new ObjectMapper();
    queueManager = mapper.convertValue(config.getQueueManagers().get(0), QueueManager.class);
    meter = otelTesting.getOpenTelemetry().getMeter("opentelemetry.io/mq");
    context =
        new MetricsCollectorContext(queueManager, pcfMessageAgent, null, new MetricsConfig(config));
  }

  @Test
  void testPublishMetrics() throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class)))
        .thenReturn(createPCFResponseForInquireChannelStatusCmd());
    classUnderTest = new ChannelMetricsCollector(meter);

    classUnderTest.accept(context);

    List<String> metricsList =
        new ArrayList<>(
            List.of(
                "mq.message.count",
                "mq.status",
                "mq.byte.sent",
                "mq.byte.received",
                "mq.buffers.sent",
                "mq.buffers.received"));

    for (MetricData metric : otelTesting.getMetrics()) {
      if (metricsList.remove(metric.getName())) {
        if (metric.getName().equals("mq.message.count")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(17);
        }

        if (metric.getName().equals("mq.status")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(3);
        }
        if (metric.getName().equals("mq.byte.sent")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(6984);
        }
        if (metric.getName().equals("mq.byte.received")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(5772);
        }
        if (metric.getName().equals("mq.buffers.sent")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(19);
        }
        if (metric.getName().equals("mq.buffers.received")) {
          assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue())
              .isEqualTo(20);
        }
      }
    }
    assertThat(metricsList).isEmpty();
  }

  /*
     Request
     PCFMessage:
     MQCFH [type: 1, strucLength: 36, version: 1, command: 42 (MQCMD_INQUIRE_CHANNEL_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 3]
     MQCFST [type: 4, strucLength: 24, parameter: 3501 (MQCACH_FIRST/MQCACH_CHANNEL_NAME), codedCharSetId: 0, stringLength: 1, string: *]
     MQCFIN [type: 3, strucLength: 16, parameter: 1523 (MQIACH_CHANNEL_INSTANCE_TYPE), value: 1011]
     MQCFIL [type: 5, strucLength: 48, parameter: 1524 (MQIACH_CHANNEL_INSTANCE_ATTRS), count: 8, values: {3501, 3506, 1527, 1534, 1538, 1535, 1539, 1536}]

     Response
     PCFMessage:
     MQCFH [type: 2, strucLength: 36, version: 1, command: 42 (MQCMD_INQUIRE_CHANNEL_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 11]
     MQCFST [type: 4, strucLength: 40, parameter: 3501 (MQCACH_FIRST/MQCACH_CHANNEL_NAME), codedCharSetId: 819, stringLength: 20, string: DEV.ADMIN.SVRCONN   ]
     MQCFIN [type: 3, strucLength: 16, parameter: 1511 (MQIACH_CHANNEL_TYPE), value: 7]
     MQCFIN [type: 3, strucLength: 16, parameter: 1539 (MQIACH_BUFFERS_RCVD/MQIACH_BUFFERS_RECEIVED), value: 20]
     MQCFIN [type: 3, strucLength: 16, parameter: 1538 (MQIACH_BUFFERS_SENT), value: 19]
     MQCFIN [type: 3, strucLength: 16, parameter: 1536 (MQIACH_BYTES_RCVD/MQIACH_BYTES_RECEIVED), value: 5772]
     MQCFIN [type: 3, strucLength: 16, parameter: 1535 (MQIACH_BYTES_SENT), value: 6984]
     MQCFST [type: 4, strucLength: 284, parameter: 3506 (MQCACH_CONNECTION_NAME), codedCharSetId: 819, stringLength: 264, string: 172.17.0.1]
     MQCFIN [type: 3, strucLength: 16, parameter: 1523 (MQIACH_CHANNEL_INSTANCE_TYPE), value: 1011]
     MQCFIN [type: 3, strucLength: 16, parameter: 1534 (MQIACH_MSGS), value: 17]
     MQCFIN [type: 3, strucLength: 16, parameter: 1527 (MQIACH_CHANNEL_STATUS), value: 3]
     MQCFIN [type: 3, strucLength: 16, parameter: 1609 (MQIACH_CHANNEL_SUBSTATE), value: 300]
  */

  private PCFMessage[] createPCFResponseForInquireChannelStatusCmd() {
    PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS, 1, true);
    response1.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "DEV.ADMIN.SVRCONN");
    response1.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, 7);
    response1.addParameter(CMQCFC.MQIACH_BUFFERS_RECEIVED, 20);
    response1.addParameter(CMQCFC.MQIACH_BUFFERS_SENT, 19);
    response1.addParameter(CMQCFC.MQIACH_BYTES_RECEIVED, 5772);
    response1.addParameter(CMQCFC.MQIACH_BYTES_SENT, 6984);
    response1.addParameter(CMQCFC.MQCACH_CONNECTION_NAME, "172.17.0.1 ");
    response1.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, 1011);
    response1.addParameter(CMQCFC.MQIACH_MSGS, 17);
    response1.addParameter(CMQCFC.MQIACH_CHANNEL_STATUS, 3);
    response1.addParameter(CMQCFC.MQIACH_CHANNEL_SUBSTATE, 300);
    response1.addParameter(CMQCFC.MQCACH_CHANNEL_START_DATE, "2012-01-03");
    response1.addParameter(CMQCFC.MQCACH_CHANNEL_START_TIME, "22.33.44");
    response1.addParameter(CMQCFC.MQCACH_MCA_JOB_NAME, "000042040000000C");

    PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS, 2, true);
    response2.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "DEV.APP.SVRCONN");
    response2.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, 7);
    response2.addParameter(CMQCFC.MQIACH_BUFFERS_RECEIVED, 20);
    response2.addParameter(CMQCFC.MQIACH_BUFFERS_SENT, 19);
    response2.addParameter(CMQCFC.MQIACH_BYTES_RECEIVED, 5772);
    response2.addParameter(CMQCFC.MQIACH_BYTES_SENT, 6984);
    response2.addParameter(CMQCFC.MQCACH_CONNECTION_NAME, "172.17.0.2 ");
    response2.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, 1011);
    response2.addParameter(CMQCFC.MQIACH_MSGS, 17);
    response2.addParameter(CMQCFC.MQIACH_CHANNEL_STATUS, 3);
    response2.addParameter(CMQCFC.MQIACH_CHANNEL_SUBSTATE, 300);
    response2.addParameter(CMQCFC.MQCACH_CHANNEL_START_DATE, "2012-01-04");
    response2.addParameter(CMQCFC.MQCACH_CHANNEL_START_TIME, "22.33.45");
    response2.addParameter(CMQCFC.MQCACH_MCA_JOB_NAME, "000042040000000D");

    PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS, 2, true);
    response3.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "TEST.APP.SVRCONN");
    response3.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, 7);
    response3.addParameter(CMQCFC.MQIACH_BUFFERS_RECEIVED, 20);
    response3.addParameter(CMQCFC.MQIACH_BUFFERS_SENT, 19);
    response3.addParameter(CMQCFC.MQIACH_BYTES_RECEIVED, 5772);
    response3.addParameter(CMQCFC.MQIACH_BYTES_SENT, 6984);
    response3.addParameter(CMQCFC.MQCACH_CONNECTION_NAME, "172.17.0.2 ");
    response3.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, 1011);
    response3.addParameter(CMQCFC.MQIACH_MSGS, 17);
    response3.addParameter(CMQCFC.MQIACH_CHANNEL_STATUS, 3);
    response3.addParameter(CMQCFC.MQIACH_CHANNEL_SUBSTATE, 300);
    response3.addParameter(CMQCFC.MQCACH_CHANNEL_START_DATE, "2012-01-05");
    response3.addParameter(CMQCFC.MQCACH_CHANNEL_START_TIME, "22.33.46");
    response3.addParameter(CMQCFC.MQCACH_MCA_JOB_NAME, "000042040000000E");

    return new PCFMessage[] {response1, response2, response3};
  }

  @Test
  void testPublishMetrics_nullResponse() throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class))).thenReturn(null);
    classUnderTest = new ChannelMetricsCollector(meter);

    classUnderTest.accept(context);
    assertThat(otelTesting.getMetrics()).isEmpty();
  }

  @Test
  void testPublishMetrics_emptyResponse() throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class))).thenReturn(new PCFMessage[] {});
    classUnderTest = new ChannelMetricsCollector(meter);

    classUnderTest.accept(context);
    assertThat(otelTesting.getMetrics()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("exceptionsToThrow")
  void testPublishMetrics_pfException(Exception exceptionToThrow) throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class))).thenThrow(exceptionToThrow);
    classUnderTest = new ChannelMetricsCollector(meter);

    classUnderTest.accept(context);

    List<MetricData> exported = otelTesting.getMetrics();
    assertThat(exported.get(0).getLongGaugeData().getPoints()).hasSize(1);
    assertThatMetric(exported.get(0), 0).hasName("mq.manager.active.channels").hasValue(0);
  }

  static Stream<Arguments> exceptionsToThrow() {
    return Stream.of(
        arguments(new RuntimeException("KBAOOM")),
        arguments(new PCFException(91, MQRCCF_CHL_STATUS_NOT_FOUND, "flimflam")),
        arguments(new PCFException(4, MQRC_SELECTOR_ERROR, "shazbot")),
        arguments(new PCFException(4, 42, "boz")));
  }
}
