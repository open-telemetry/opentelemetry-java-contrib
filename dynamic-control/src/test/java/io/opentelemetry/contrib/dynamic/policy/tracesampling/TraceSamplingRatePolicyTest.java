/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

class TraceSamplingRatePolicyTest {

  @Test
  void constructorStoresProbabilityAndType() {
    TraceSamplingRatePolicy policy = new TraceSamplingRatePolicy(0.25);

    assertThat(policy.getIdentity()).isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY);
    assertThat(policy.getProbability()).isEqualTo(0.25);
    assertThat(policy.getType()).isEqualTo(TraceSamplingRatePolicy.POLICY_TYPE);
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.CUSTOM);
  }

  @Test
  void constructorStoresExplicitSourceKind() {
    TraceSamplingRatePolicy policy = new TraceSamplingRatePolicy(0.25, SourceKind.OPAMP);

    assertThat(policy.getIdentity()).isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY);
    assertThat(policy.getProbability()).isEqualTo(0.25);
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.OPAMP);
  }

  @Test
  void constructorNormalizesNegativeZeroToPositiveZero() {
    TraceSamplingRatePolicy negativeZero = new TraceSamplingRatePolicy(-0.0);
    TraceSamplingRatePolicy positiveZero = new TraceSamplingRatePolicy(0.0);

    assertThat(negativeZero.getProbability()).isEqualTo(0.0);
    assertThat(Double.doubleToRawLongBits(negativeZero.getProbability()))
        .isEqualTo(Double.doubleToRawLongBits(0.0));
    assertThat(positiveZero.getProbability()).isEqualTo(0.0);
  }

  @Test
  void constructorRejectsOutOfRangeOrNaNProbabilities() {
    assertThatThrownBy(() -> new TraceSamplingRatePolicy(Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> new TraceSamplingRatePolicy(-0.001))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> new TraceSamplingRatePolicy(1.001))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
  }

  @Test
  void initializeRejectsNullCustomizer() {
    assertThatThrownBy(() -> TraceSamplingRatePolicy.initialize(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("autoConfiguration cannot be null");
  }

  @Test
  void initializeStoresDelegatingSamplerAndRegistersCustomizer() {
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    TraceSamplingRatePolicy.initialize(customizer);

    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
    verify(customizer).addSamplerCustomizer(any());
  }

  @Test
  void createSamplerAcceptsBoundaryProbabilities() {
    Sampler zero = TraceSamplingRatePolicy.createSampler(0.0);
    Sampler negativeZero = TraceSamplingRatePolicy.createSampler(-0.0);
    Sampler one = TraceSamplingRatePolicy.createSampler(1.0);

    assertThat(zero).isNotNull();
    assertThat(negativeZero).isNotNull();
    assertThat(one).isNotNull();
  }

  @Test
  void createSamplerRejectsOutOfRangeOrNaNProbabilities() {
    assertThatThrownBy(() -> TraceSamplingRatePolicy.createSampler(Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> TraceSamplingRatePolicy.createSampler(-0.01))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> TraceSamplingRatePolicy.createSampler(1.01))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
  }
}
