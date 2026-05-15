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
class TopicMetricsCollectorTest {

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
    MetricProducer producer =
        new MetricProducer(Resource.empty(), InstrumentationScopeInfo.empty());
    classUnderTest = new TopicMetricsCollector(producer);

    when(pcfMessageAgent.send(any(PCFMessage.class)))
        .thenReturn(createPCFResponseForInquireTopicStatusCmd());

    classUnderTest.accept(context);

    List<MetricData> result = producer.produce(Resource.empty());
    assertThat(result.size()).isEqualTo(4);
    assertThat(result.get(0).getName()).isEqualTo("ibm.mq.publish.count");
    assertThat(result.get(0).getLongGaugeData().getPoints().iterator().next().getValue())
        .isEqualTo(2L);

    assertThat(result.get(1).getName()).isEqualTo("ibm.mq.subscription.count");
    assertThat(result.get(1).getLongGaugeData().getPoints().iterator().next().getValue())
        .isEqualTo(3L);

    assertThat(result.get(2).getName()).isEqualTo("ibm.mq.publish.count");
    assertThat(result.get(2).getLongGaugeData().getPoints().iterator().next().getValue())
        .isEqualTo(3L);

    assertThat(result.get(3).getName()).isEqualTo("ibm.mq.subscription.count");
    assertThat(result.get(3).getLongGaugeData().getPoints().iterator().next().getValue())
        .isEqualTo(4L);
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
