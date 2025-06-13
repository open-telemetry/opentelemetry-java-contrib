/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.ibm.mq.config.QueueManager;
import io.opentelemetry.ibm.mq.metrics.MetricsConfig;
import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueueManagerMetricsCollectorTest {

  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  QueueManagerMetricsCollector classUnderTest;
  QueueManager queueManager;
  MetricsCollectorContext context;
  @Mock PCFMessageAgent pcfMessageAgent;

  @BeforeEach
  public void setup() throws Exception {

    ConfigWrapper config = ConfigWrapper.parse("src/test/resources/conf/config.yml");
    ObjectMapper mapper = new ObjectMapper();
    queueManager = mapper.convertValue(config.getQueueManagers().get(0), QueueManager.class);
    context =
        new MetricsCollectorContext(queueManager, pcfMessageAgent, null, new MetricsConfig(config));
  }

  @Test
  public void testProcessPCFRequestAndPublishQMetricsForInquireQStatusCmd() throws Exception {
    when(pcfMessageAgent.send(any(PCFMessage.class)))
        .thenReturn(createPCFResponseForInquireQMgrStatusCmd());
    classUnderTest =
        new QueueManagerMetricsCollector(
            otelTesting.getOpenTelemetry().getMeter("opentelemetry.io/mq"));
    classUnderTest.accept(context);
    List<String> metricsList = new ArrayList<>(List.of("mq.manager.status"));

    for (MetricData metric : otelTesting.getMetrics()) {
      if (metricsList.remove(metric.getName())) {
        assertThat(metric.getLongGaugeData().getPoints().iterator().next().getValue()).isEqualTo(2);
      }
    }
    assertThat(metricsList).isEmpty();
  }

  /*  Request
     PCFMessage:
     MQCFH [type: 1, strucLength: 36, version: 1, command: 161 (MQCMD_INQUIRE_Q_MGR_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 1]
     MQCFIL [type: 5, strucLength: 20, parameter: 1229 (MQIACF_Q_MGR_STATUS_ATTRS), count: 1, values: {1009}]

     Response
     PCFMessage:
     MQCFH [type: 2, strucLength: 36, version: 1, command: 161 (MQCMD_INQUIRE_Q_MGR_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 23]
     MQCFST [type: 4, strucLength: 68, parameter: 2015 (MQCA_Q_MGR_NAME), codedCharSetId: 819, stringLength: 48, string: QM1                                             ]
     MQCFIN [type: 3, strucLength: 16, parameter: 1149 (MQIACF_Q_MGR_STATUS), value: 2]
     MQCFST [type: 4, strucLength: 20, parameter: 3208 (null), codedCharSetId: 819, stringLength: 0, string: ]
     MQCFIN [type: 3, strucLength: 16, parameter: 1416 (null), value: 0]
     MQCFIN [type: 3, strucLength: 16, parameter: 1232 (MQIACF_CHINIT_STATUS), value: 2]
     MQCFIN [type: 3, strucLength: 16, parameter: 1233 (MQIACF_CMD_SERVER_STATUS), value: 2]
     MQCFIN [type: 3, strucLength: 16, parameter: 1230 (MQIACF_CONNECTION_COUNT), value: 23]
     MQCFST [type: 4, strucLength: 20, parameter: 3071 (MQCACF_CURRENT_LOG_EXTENT_NAME), codedCharSetId: 819, stringLength: 0, string: ]
     MQCFST [type: 4, strucLength: 20, parameter: 2115 (null), codedCharSetId: 819, stringLength: 0, string: ]
     MQCFST [type: 4, strucLength: 36, parameter: 2116 (null), codedCharSetId: 819, stringLength: 13, string: Installation1]
     MQCFST [type: 4, strucLength: 28, parameter: 2117 (null), codedCharSetId: 819, stringLength: 8, string: /opt/mqm]
     MQCFIN [type: 3, strucLength: 16, parameter: 1409 (null), value: 0]
     MQCFIN [type: 3, strucLength: 16, parameter: 1420 (null), value: 9]
     MQCFST [type: 4, strucLength: 44, parameter: 3074 (MQCACF_LOG_PATH), codedCharSetId: 819, stringLength: 24, string: /var/mqm/log/QM1/active/]
     MQCFIN [type: 3, strucLength: 16, parameter: 1421 (null), value: 9]
     MQCFST [type: 4, strucLength: 20, parameter: 3073 (MQCACF_MEDIA_LOG_EXTENT_NAME), codedCharSetId: 819, stringLength: 0, string: ]
     MQCFIN [type: 3, strucLength: 16, parameter: 1417 (null), value: 0]
     MQCFST [type: 4, strucLength: 20, parameter: 3072 (MQCACF_RESTART_LOG_EXTENT_NAME), codedCharSetId: 819, stringLength: 0, string: ]
     MQCFIN [type: 3, strucLength: 16, parameter: 1418 (null), value: 1]
     MQCFIN [type: 3, strucLength: 16, parameter: 1419 (null), value: 0]
     MQCFIN [type: 3, strucLength: 16, parameter: 1325 (null), value: 0]
     MQCFST [type: 4, strucLength: 32, parameter: 3175 (null), codedCharSetId: 819, stringLength: 12, string: 2018-05-08  ]
     MQCFST [type: 4, strucLength: 28, parameter: 3176 (null), codedCharSetId: 819, stringLength: 8, string: 08.32.08]
  */

  private PCFMessage[] createPCFResponseForInquireQMgrStatusCmd() {
    PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS, 1, true);
    response1.addParameter(CMQC.MQCA_Q_MGR_NAME, "QM1");
    response1.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS, 2);
    response1.addParameter(CMQCFC.MQIACF_CHINIT_STATUS, 2);
    response1.addParameter(CMQCFC.MQIACF_CMD_SERVER_STATUS, 2);
    response1.addParameter(CMQCFC.MQIACF_CONNECTION_COUNT, 23);
    response1.addParameter(CMQCFC.MQCACF_CURRENT_LOG_EXTENT_NAME, "");
    response1.addParameter(CMQCFC.MQCACF_LOG_PATH, "/var/mqm/log/QM1/active/");
    response1.addParameter(CMQCFC.MQCACF_MEDIA_LOG_EXTENT_NAME, "");
    response1.addParameter(CMQCFC.MQCACF_RESTART_LOG_EXTENT_NAME, "");
    response1.addParameter(CMQCFC.MQIACF_RESTART_LOG_SIZE, 42);
    response1.addParameter(CMQCFC.MQIACF_REUSABLE_LOG_SIZE, 42);
    response1.addParameter(CMQCFC.MQIACF_ARCHIVE_LOG_SIZE, 42);

    return new PCFMessage[] {response1};
  }
}
