/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyInit;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Declarative sampler component that bootstraps top-level telemetry policy wiring. */
@AutoService(ComponentProvider.class)
public final class TelemetryPolicySamplerComponentProvider implements ComponentProvider {
  private static final Logger logger =
      Logger.getLogger(TelemetryPolicySamplerComponentProvider.class.getName());
  public static final String NAME = "telemetry_policy/development";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Sampler create(DeclarativeConfigProperties config) {
    logger.log(
        Level.INFO,
        "Dynamic control extension has been loaded by the agent via declarative config");
    try {
      PolicyInit.initFromDeclarativeConfig(config);
    } catch (IllegalArgumentException e) {
      logger.log(Level.WARNING, "Failed to initialize telemetry policy from component config", e);
    }
    // TODO: install specifically a delegating sampler, and allow it to be dynamically updated by
    // the policy configuration
    // but for now just use the existing sampling rate sampler which is a specifically configured
    // delegating sampler
    Sampler initialized = TraceSamplingRatePolicy.getInitializedSampler();
    return initialized == null ? Sampler.parentBased(Sampler.alwaysOn()) : initialized;
  }

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }
}
