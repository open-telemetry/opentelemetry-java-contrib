/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class AbstractSourcePolicyValidatorTest {

  @Test
  void fullPolicyPassesOnlyGenericKeepValueToConcreteValidator() {
    List<SourceWrapper> sources =
        SourceFormat.JSONKEYVALUE.parse(
            "{\"id\":\"metric-exporter-stop\",\"name\":\"Stop metric export\","
                + "\"metric\":{\"match\":[{\"metric_field\":\"name\",\"exists\":true}],"
                + "\"keep\":false}}",
            Collections.singleton("metric-exporter-stop"));
    RecordingMetricValidator validator = new RecordingMetricValidator();

    TelemetryPolicy policy = validator.validate(sources.get(0), SourceKind.CUSTOM);

    assertThat(policy).isNotNull();
    assertThat(validator.receivedValue).isNotNull();
    assertThat(validator.receivedValue.isBoolean()).isTrue();
    assertThat(validator.receivedValue.asBoolean()).isFalse();
  }

  private static final class RecordingMetricValidator extends AbstractSourcePolicyValidator {
    @Nullable private JsonNode receivedValue;

    @Override
    public String getPolicyType() {
      return "metric-exporter-stop";
    }

    @Override
    protected TelemetryPolicy validateJsonValue(JsonNode valueNode, SourceKind sourceKind) {
      receivedValue = valueNode;
      return mock(TelemetryPolicy.class);
    }

    @Override
    protected TelemetryPolicy validateKeyValueValue(String value, SourceKind sourceKind) {
      return null;
    }
  }
}
