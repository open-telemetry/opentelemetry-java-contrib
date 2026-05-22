/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  void updatePoliciesReturnsFalseWhenOnlyOrderDiffers() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> first =
        Arrays.asList(new TraceSamplingRatePolicy(0.1), new TraceSamplingRatePolicy(0.2));
    List<TelemetryPolicy> reordered =
        Arrays.asList(new TraceSamplingRatePolicy(0.2), new TraceSamplingRatePolicy(0.1));

    assertThat(store.updatePolicies(first)).isTrue();
    assertThat(store.updatePolicies(reordered)).isFalse();
    assertThat(store.getPolicies()).isEqualTo(first);
  }

  @Test
  void updatePoliciesIgnoresDuplicatePoliciesInInput() {
    PolicyStore store = new PolicyStore();
    TraceSamplingRatePolicy p = new TraceSamplingRatePolicy(0.5);
    assertThat(store.updatePolicies(Arrays.asList(p, new TraceSamplingRatePolicy(0.5)))).isTrue();
    assertThat(store.getPolicies()).containsExactly(p);
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.5)))).isFalse();
  }

  @Test
  void getPoliciesReturnsEmptyWhenNeverUpdated() {
    assertThat(new PolicyStore().getPolicies()).isEqualTo(Collections.emptyList());
  }

  @Test
  void registerImplementerReceivesCurrentRelevantPolicies() {
    PolicyStore store = new PolicyStore();
    store.updatePolicies(
        Arrays.asList(new TraceSamplingRatePolicy(0.5), new TelemetryPolicy("other-policy")));

    PolicyImplementer implementer = mock(PolicyImplementer.class);
    PolicyValidator validator = mock(PolicyValidator.class);
    when(validator.getPolicyType()).thenReturn(TraceSamplingRatePolicy.POLICY_TYPE);
    when(implementer.getValidators()).thenReturn(singletonList(validator));

    store.registerImplementer(implementer);

    verify(implementer).onPoliciesChanged(singletonList(new TraceSamplingRatePolicy(0.5)));
  }

  @Test
  void updatePoliciesNotifiesRegisteredImplementerWithRelevantPolicies() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();

    store.registerImplementer(implementer);
    clearInvocations(implementer);
    store.updatePolicies(
        Arrays.asList(new TelemetryPolicy("other-policy"), new TraceSamplingRatePolicy(0.25)));

    verify(implementer).onPoliciesChanged(singletonList(new TraceSamplingRatePolicy(0.25)));
  }

  @Test
  void updatePoliciesContinuesWhenImplementerThrows() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer failingImplementer = traceSamplingImplementer();
    PolicyImplementer nextImplementer = traceSamplingImplementer();
    List<TelemetryPolicy> updatedPolicies = singletonList(new TraceSamplingRatePolicy(0.25));
    doThrow(new IllegalStateException("boom"))
        .when(failingImplementer)
        .onPoliciesChanged(updatedPolicies);

    store.registerImplementer(failingImplementer);
    store.registerImplementer(nextImplementer);
    clearInvocations(failingImplementer, nextImplementer);

    assertThat(store.updatePolicies(updatedPolicies)).isTrue();

    verify(failingImplementer).onPoliciesChanged(updatedPolicies);
    verify(nextImplementer).onPoliciesChanged(updatedPolicies);
    assertThat(store.getPolicies()).isEqualTo(updatedPolicies);
  }

  @Test
  void registerImplementerContinuesAfterPreviousImplementerThrows() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> currentPolicies = singletonList(new TraceSamplingRatePolicy(0.5));
    assertThat(store.updatePolicies(currentPolicies)).isTrue();

    PolicyImplementer failingImplementer = traceSamplingImplementer();
    PolicyImplementer nextImplementer = traceSamplingImplementer();
    doThrow(new IllegalStateException("boom"))
        .when(failingImplementer)
        .onPoliciesChanged(currentPolicies);

    store.registerImplementer(failingImplementer);
    store.registerImplementer(nextImplementer);

    verify(failingImplementer).onPoliciesChanged(currentPolicies);
    verify(nextImplementer).onPoliciesChanged(currentPolicies);
  }

  private static PolicyImplementer traceSamplingImplementer() {
    PolicyImplementer implementer = mock(PolicyImplementer.class);
    PolicyValidator validator = mock(PolicyValidator.class);
    when(validator.getPolicyType()).thenReturn(TraceSamplingRatePolicy.POLICY_TYPE);
    when(implementer.getValidators()).thenReturn(singletonList(validator));
    return implementer;
  }
}
