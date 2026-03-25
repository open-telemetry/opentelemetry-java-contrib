/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyStoreTest {

  @Test
  void updatePoliciesReturnsTrueOnFirstSet() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> policies = singletonList(new TraceSamplingRatePolicy(0.5));

    assertThat(store.updatePolicies(policies)).isTrue();
    assertThat(store.getPolicies()).isEqualTo(policies);
  }

  @Test
  void updatePoliciesReturnsFalseWhenEqualContent() {
    PolicyStore store = new PolicyStore();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.5)))).isTrue();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.5)))).isFalse();
  }

  @Test
  void updatePoliciesReturnsTrueWhenProbabilityChanges() {
    PolicyStore store = new PolicyStore();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.25)))).isTrue();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.75)))).isTrue();
    assertThat(store.getPolicies()).containsExactly(new TraceSamplingRatePolicy(0.75));
  }

  @Test
  void updatePoliciesDetectsListOrderChange() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> first =
        Arrays.asList(new TraceSamplingRatePolicy(0.1), new TraceSamplingRatePolicy(0.2));
    List<TelemetryPolicy> reordered =
        Arrays.asList(new TraceSamplingRatePolicy(0.2), new TraceSamplingRatePolicy(0.1));

    assertThat(store.updatePolicies(first)).isTrue();
    assertThat(store.updatePolicies(reordered)).isTrue();
    assertThat(store.getPolicies()).isEqualTo(reordered);
  }

  @Test
  void getPoliciesReturnsEmptyWhenNeverUpdated() {
    assertThat(new PolicyStore().getPolicies()).isEqualTo(Collections.emptyList());
  }
}
