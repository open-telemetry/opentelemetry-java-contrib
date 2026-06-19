/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import javax.annotation.Nonnull;

/**
 * NOTE PolicyInit not yet added - remove this note when it is added
 *
 * <p>Initializes one policy type and returns the implementer that applies that type at runtime.
 *
 * <p>This is used by {@code
 * io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit.registerPolicyType(...)} and is
 * typically provided as a method reference, for example:
 *
 * <pre>{@code
 * io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit.registerPolicyType(
 *     TraceSamplingRatePolicy.POLICY_TYPE,
 *     TraceSamplingRatePolicy.class,
 *     TraceSamplingRatePolicy::initialize);
 * }</pre>
 */
@FunctionalInterface
public interface PolicyTypeInitializer {
  /**
   * Initializes policy runtime wiring for one policy type.
   *
   * @param autoConfiguration OpenTelemetry auto-configuration customizer
   * @return non-null implementer for the policy type
   */
  @Nonnull
  PolicyImplementer initialize(AutoConfigurationCustomizer autoConfiguration);
}
