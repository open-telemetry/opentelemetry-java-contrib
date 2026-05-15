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
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricProducer;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListenerMetricsCollectorTest {

  ListenerMetricsCollector classUnderTest;
  QueueManager queueManager;
  ConfigWrapper config;
  @Mock private PCFMessageAgent pcfMessageAgent;

  @BeforeEach
  public void setup() throws Exception {
    config = ConfigWrapper.parse("src/test/resources/conf/config.yml");
    ObjectMapper mapper = new ObjectMapper();
    queueManager = mapper.convertValue(config.getQueueManagers().get(0), QueueManager.class);
  }

  @Test
  void testPublishMetrics() throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class)))
        .thenReturn(createPCFResponseForInquireListenerStatusCmd());

    MetricsCollectorContext context =
        new MetricsCollectorContext(queueManager, pcfMessageAgent, null, new MetricsConfig(config));
    MetricProducer producer =
        new MetricProducer(Resource.empty(), InstrumentationScopeInfo.empty());
    classUnderTest = new ListenerMetricsCollector(producer);
    classUnderTest.accept(context);

    List<MetricData> result = producer.produce(Resource.empty());
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0).getName()).isEqualTo("ibm.mq.listener.status");
    assertThat(result.get(0).getLongGaugeData().getPoints().iterator().next().getValue())
        .isEqualTo(2L);

    assertThat(result.get(1).getName()).isEqualTo("ibm.mq.listener.status");
    assertThat(result.get(1).getLongGaugeData().getPoints().iterator().next().getValue())
        .isEqualTo(3L);
  }

  /*
     Request
     PCFMessage:
     MQCFH [type: 1, strucLength: 36, version: 1, command: 98 (MQCMD_INQUIRE_LISTENER_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 2]
     MQCFST [type: 4, strucLength: 24, parameter: 3554 (MQCACH_LISTENER_NAME), codedCharSetId: 0, stringLength: 1, string: *]
     MQCFIL [type: 5, strucLength: 24, parameter: 1223 (MQIACF_LISTENER_STATUS_ATTRS), count: 2, values: {3554, 1599}]

     Response
     PCFMessage:
     MQCFH [type: 2, strucLength: 36, version: 1, command: 98 (MQCMD_INQUIRE_LISTENER_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 2]
     MQCFST [type: 4, strucLength: 48, parameter: 3554 (MQCACH_LISTENER_NAME), codedCharSetId: 819, stringLength: 27, string: SYSTEM.DEFAULT.LISTENER.TCP]
     MQCFIN [type: 3, strucLength: 16, parameter: 1599 (MQIACH_LISTENER_STATUS), value: 2]
  */

  private static PCFMessage[] createPCFResponseForInquireListenerStatusCmd() {
    PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS, 1, true);
    response1.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "DEV.DEFAULT.LISTENER.TCP");
    response1.addParameter(CMQCFC.MQIACH_LISTENER_STATUS, 2);

    PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS, 2, true);
    response2.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "DEV.LISTENER.TCP");
    response2.addParameter(CMQCFC.MQIACH_LISTENER_STATUS, 3);

    PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS, 3, true);
    response3.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "SYSTEM.LISTENER.TCP");
    response3.addParameter(CMQCFC.MQIACH_LISTENER_STATUS, 1);

    return new PCFMessage[] {response1, response2, response3};
  }
}
