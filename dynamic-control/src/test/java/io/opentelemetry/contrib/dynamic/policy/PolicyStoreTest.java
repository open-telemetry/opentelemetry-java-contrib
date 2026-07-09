/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
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
  void updatePoliciesAcceptsRepeatedEquivalentSnapshot() {
    PolicyStore store = new PolicyStore();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.5)))).isTrue();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.5)))).isTrue();
  }

  @Test
  void updatePoliciesReturnsTrueWhenProbabilityChanges() {
    PolicyStore store = new PolicyStore();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.25)))).isTrue();
    assertThat(store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.75)))).isTrue();
    assertThat(store.getPolicies()).hasSize(1);
    assertThat(((TraceSamplingRatePolicy) store.getPolicies().get(0)).getProbability())
        .isEqualTo(0.75);
  }

  @Test
  void updatePoliciesAcceptsReorderedSnapshot() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> first =
        Arrays.asList(
            new TestTelemetryPolicy(
                new TelemetryPolicyIdentity("first-policy", "First policy"), "test-policy"),
            new TestTelemetryPolicy(
                new TelemetryPolicyIdentity("second-policy", "Second policy"), "test-policy"));
    List<TelemetryPolicy> reordered = Arrays.asList(first.get(1), first.get(0));

    assertThat(store.updatePolicies(first)).isTrue();
    assertThat(store.updatePolicies(reordered)).isTrue();
    assertThat(store.getPolicies()).containsExactly(reordered.get(0), reordered.get(1));
  }

  @Test
  void updatePoliciesIgnoresDuplicatePolicyIdentitiesInInput() {
    PolicyStore store = new PolicyStore();
    TelemetryPolicy first =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "Test policy"), "test-policy");
    TelemetryPolicy duplicate =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "Updated name"), "test-policy");

    assertThat(store.updatePolicies(Arrays.asList(first, duplicate))).isTrue();

    assertThat(store.getPolicies()).containsExactly(first);
  }

  @Test
  void updatePoliciesResolvesDuplicatePolicyIdentitiesBySourcePriority() {
    PolicyStore store = new PolicyStore();
    TelemetryPolicy filePolicy =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "File policy"),
            "test-policy",
            SourceKind.FILE);
    TelemetryPolicy httpPolicy =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "HTTP policy"),
            "test-policy",
            SourceKind.HTTP);
    TelemetryPolicy opampPolicy =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "OpAMP policy"),
            "test-policy",
            SourceKind.OPAMP);

    assertThat(store.updatePolicies(Arrays.asList(filePolicy, opampPolicy, httpPolicy))).isTrue();

    assertThat(store.getPolicies()).containsExactly(opampPolicy);
  }

  @Test
  void updatePoliciesUsesHttpWhenDuplicatePolicyHasNoOpampSource() {
    PolicyStore store = new PolicyStore();
    TelemetryPolicy filePolicy =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "File policy"),
            "test-policy",
            SourceKind.FILE);
    TelemetryPolicy httpPolicy =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy", "HTTP policy"),
            "test-policy",
            SourceKind.HTTP);

    assertThat(store.updatePolicies(Arrays.asList(filePolicy, httpPolicy))).isTrue();

    assertThat(store.getPolicies()).containsExactly(httpPolicy);
  }

  @Test
  void getPoliciesReturnsEmptyWhenNeverUpdated() {
    assertThat(new PolicyStore().getPolicies()).isEqualTo(Collections.emptyList());
  }

  @Test
  void registerImplementerReceivesCurrentRelevantPolicies() {
    PolicyStore store = new PolicyStore();
    store.updatePolicies(Arrays.asList(new TraceSamplingRatePolicy(0.5), unrelatedPolicy()));

    PolicyImplementer implementer = mock(PolicyImplementer.class);
    PolicyValidator validator = mock(PolicyValidator.class);
    when(validator.getPolicyType()).thenReturn(TraceSamplingRatePolicy.POLICY_TYPE);
    when(implementer.getValidators()).thenReturn(singletonList(validator));

    store.registerImplementer(implementer);

    verify(implementer)
        .onPoliciesChanged(argThat(policies -> containsTraceSamplingProbability(policies, 0.5)));
  }

  @Test
  void updatePoliciesNotifiesRegisteredImplementerWithRelevantPolicies() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();

    store.registerImplementer(implementer);
    clearInvocations(implementer);
    store.updatePolicies(Arrays.asList(unrelatedPolicy(), new TraceSamplingRatePolicy(0.25)));

    verify(implementer)
        .onPoliciesChanged(argThat(policies -> containsTraceSamplingProbability(policies, 0.25)));
  }

  @Test
  void updatePoliciesNotifiesDeletedPolicyWhenPolicyDisappears() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();
    TraceSamplingRatePolicy removedPolicy = new TraceSamplingRatePolicy(0.5);
    store.updatePolicies(singletonList(removedPolicy));
    store.registerImplementer(implementer);
    clearInvocations(implementer);

    assertThat(store.updatePolicies(Collections.emptyList())).isTrue();

    verify(implementer)
        .onPoliciesChanged(
            argThat(
                policies ->
                    policies.size() == 1
                        && policies.get(0) instanceof DeletedTelemetryPolicy
                        && policies.get(0).isDeleted()
                        && ((DeletedTelemetryPolicy) policies.get(0))
                            .getIdentity()
                            .equals(removedPolicy.getIdentity())
                        && policies.get(0).getType().equals(removedPolicy.getType())));
    assertThat(store.getPolicies()).isEmpty();
  }

  @Test
  void updatePoliciesDoesNotNotifyDeletedPolicyWhenPolicyValueChangesWithSameIdentity() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();
    TraceSamplingRatePolicy updatedPolicy = new TraceSamplingRatePolicy(0.75);
    store.updatePolicies(singletonList(new TraceSamplingRatePolicy(0.5)));
    store.registerImplementer(implementer);
    clearInvocations(implementer);

    assertThat(store.updatePolicies(singletonList(updatedPolicy))).isTrue();

    verify(implementer).onPoliciesChanged(singletonList(updatedPolicy));
  }

  @Test
  void updatePoliciesNotifiesDeletedPolicyForAnyIdentityBearingPolicy() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = implementerFor("test-policy");
    TestTelemetryPolicy removedPolicy =
        new TestTelemetryPolicy(
            new TelemetryPolicyIdentity("test-policy-id", "Test policy"), "test-policy");
    store.updatePolicies(singletonList(removedPolicy));
    store.registerImplementer(implementer);
    clearInvocations(implementer);

    assertThat(store.updatePolicies(Collections.emptyList())).isTrue();

    verify(implementer)
        .onPoliciesChanged(
            argThat(
                policies ->
                    policies.size() == 1
                        && policies.get(0) instanceof DeletedTelemetryPolicy
                        && policies.get(0).isDeleted()
                        && ((DeletedTelemetryPolicy) policies.get(0))
                            .getIdentity()
                            .equals(removedPolicy.getIdentity())
                        && policies.get(0).getType().equals(removedPolicy.getType())));
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
    return implementerFor(TraceSamplingRatePolicy.POLICY_TYPE);
  }

  private static PolicyImplementer implementerFor(String policyType) {
    PolicyImplementer implementer = mock(PolicyImplementer.class);
    PolicyValidator validator = mock(PolicyValidator.class);
    when(validator.getPolicyType()).thenReturn(policyType);
    when(implementer.getValidators()).thenReturn(singletonList(validator));
    return implementer;
  }

  private static TelemetryPolicy unrelatedPolicy() {
    return new TestTelemetryPolicy(
        new TelemetryPolicyIdentity("other-policy", "Other policy"), "other-policy");
  }

  private static boolean containsTraceSamplingProbability(
      List<TelemetryPolicy> policies, double probability) {
    if (policies.size() != 1 || !(policies.get(0) instanceof TraceSamplingRatePolicy)) {
      return false;
    }
    TraceSamplingRatePolicy policy = (TraceSamplingRatePolicy) policies.get(0);
    return Double.compare(policy.getProbability(), probability) == 0;
  }

  private static final class TestTelemetryPolicy implements TelemetryPolicy {
    private final TelemetryPolicyIdentity identity;
    private final String type;
    private final SourceKind sourceKind;

    private TestTelemetryPolicy(TelemetryPolicyIdentity identity, String type) {
      this(identity, type, SourceKind.CUSTOM);
    }

    private TestTelemetryPolicy(
        TelemetryPolicyIdentity identity, String type, SourceKind sourceKind) {
      this.identity = identity;
      this.type = type;
      this.sourceKind = sourceKind;
    }

    @Override
    public TelemetryPolicyIdentity getIdentity() {
      return identity;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public SourceKind getSourceKind() {
      return sourceKind;
    }
  }
}
