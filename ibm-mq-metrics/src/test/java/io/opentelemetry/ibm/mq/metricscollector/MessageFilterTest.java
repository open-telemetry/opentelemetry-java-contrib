/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.ibm.mq.config.ExcludeFilters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class MessageFilterTest {

  @Test
  void testMessageFilterWithQueueNames() throws PCFException {

    ExcludeFilters filter = new ExcludeFilters();
    filter.setType("STARTSWITH");
    Set<String> values = new HashSet<>();
    values.add("FOO");
    values.add("BAR");
    filter.setValues(values);

    Set<ExcludeFilters> filters = new HashSet<>();
    filters.add(filter);

    List<PCFMessage> response = new ArrayList<>();
    PCFMessage message = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q, 1, true);
    message.addParameter(CMQC.MQCA_Q_NAME, "DEV.QUEUE");
    response.add(message);
    PCFMessage message2 = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
    message2.addParameter(CMQC.MQCA_Q_NAME, "FOO.QUEUE");
    response.add(message2);
    PCFMessage message3 = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
    message3.addParameter(CMQC.MQCA_Q_NAME, "DEV.BAR.QUEUE");
    response.add(message3);

    List<PCFMessage> filtered =
        MessageFilter.ofKind("queue")
            .excluding(filters)
            .withResourceExtractor(MessageBuddy::queueName)
            .filter(response);

    assertThat(filtered).contains(message);
    assertThat(filtered).doesNotContain(message2);
    assertThat(filtered).contains(message3);
  }
}
