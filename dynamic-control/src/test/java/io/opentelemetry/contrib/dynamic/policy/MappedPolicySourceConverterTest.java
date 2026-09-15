/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingValidator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class MappedPolicySourceConverterTest {

  @Test
  void convertsAndRemapsFullPolicyObjectWithoutMutatingSource() {
    MappedPolicySourceConverter converter =
        MappedPolicySourceConverter.create(
            Collections.singletonList(
                new PolicySourceMappingConfig("external-trace-policy", "trace-sampling")),
            Collections.singletonList(new TraceSamplingValidator()));
    List<SourceWrapper> sources =
        SourceFormat.JSONKEYVALUE.parse(
            "{"
                + "\"id\":\"external-trace-policy\","
                + "\"name\":\"Trace sampling rate\","
                + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true}],"
                + "\"keep\":{\"probability\":0.1}}"
                + "}",
            converter.getMappedPolicyIds());

    TelemetryPolicy converted = converter.convert(sources.get(0), SourceKind.OPAMP);

    assertThat(converted).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) converted).getProbability()).isCloseTo(0.1, within(1e-9));
    assertThat(((JsonSourceWrapper) sources.get(0)).asJsonNode().get("id").asText())
        .isEqualTo("external-trace-policy");
  }
}
