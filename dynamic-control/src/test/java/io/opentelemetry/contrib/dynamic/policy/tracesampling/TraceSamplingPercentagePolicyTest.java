/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TraceSamplingPercentagePolicyTest {

  @AfterEach
  void tearDown() {
    AbstractTraceSamplingPolicy.resetForTest();
  }

  @Test
  void constructorStoresPercentageAndConvertsToProbability() {
    TraceSamplingPercentagePolicy policy =
        new TraceSamplingPercentagePolicy(25.0, SourceKind.CUSTOM);

    assertThat(policy.getIdentity()).isEqualTo(TraceSamplingPercentagePolicy.DEFAULT_IDENTITY);
    assertThat(policy.getPercentage()).isEqualTo(25.0);
    assertThat(policy.getSamplingProbability()).isEqualTo(0.25);
    assertThat(policy.getType()).isEqualTo(TraceSamplingPercentagePolicy.POLICY_TYPE);
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.CUSTOM);
  }

  @Test
  void constructorAcceptsBoundaryPercentages() {
    assertThat(new TraceSamplingPercentagePolicy(0.0, SourceKind.CUSTOM).getSamplingProbability())
        .isEqualTo(0.0);
    assertThat(new TraceSamplingPercentagePolicy(100.0, SourceKind.CUSTOM).getSamplingProbability())
        .isEqualTo(1.0);
  }

  @Test
  void constructorNormalizesNegativeZero() {
    TraceSamplingPercentagePolicy policy =
        new TraceSamplingPercentagePolicy(-0.0, SourceKind.CUSTOM);

    assertThat(Double.doubleToRawLongBits(policy.getPercentage()))
        .isEqualTo(Double.doubleToRawLongBits(0.0));
  }

  @Test
  void constructorRejectsOutOfRangeOrNaNPercentages() {
    assertThatThrownBy(() -> new TraceSamplingPercentagePolicy(Double.NaN, SourceKind.CUSTOM))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("percentage must be within [0.0, 100.0]");
    assertThatThrownBy(() -> new TraceSamplingPercentagePolicy(-0.001, SourceKind.CUSTOM))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("percentage must be within [0.0, 100.0]");
    assertThatThrownBy(() -> new TraceSamplingPercentagePolicy(100.001, SourceKind.CUSTOM))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("percentage must be within [0.0, 100.0]");
  }

  @Test
  void initializeStoresSharedSamplerAndRegistersCustomizer() {
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    TraceSamplingPercentagePolicy.initialize(customizer);

    assertThat(AbstractTraceSamplingPolicy.getInitializedSampler()).isNotNull();
    verify(customizer).addSamplerCustomizer(any());
  }

  @Test
  void ratioAndPercentageInitializersShareSamplerButExposeTheirOwnValidators() {
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    PolicyImplementer ratioImplementer = TraceSamplingRatePolicy.initialize(customizer);
    DelegatingSampler ratioSampler = AbstractTraceSamplingPolicy.getInitializedSampler();
    PolicyImplementer percentageImplementer = TraceSamplingPercentagePolicy.initialize(customizer);

    assertThat(AbstractTraceSamplingPolicy.getInitializedSampler()).isSameAs(ratioSampler);
    assertThat(ratioImplementer.getValidators())
        .extracting(validator -> validator.getPolicyType())
        .containsExactly(TraceSamplingRatePolicy.POLICY_TYPE);
    assertThat(percentageImplementer.getValidators())
        .extracting(validator -> validator.getPolicyType())
        .containsExactly(TraceSamplingPercentagePolicy.POLICY_TYPE);
    verify(customizer, times(1)).addSamplerCustomizer(any());
  }
}
