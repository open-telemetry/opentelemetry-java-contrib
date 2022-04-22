/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.extension;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.javaagent.tooling.HelperInjector;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Configures a static instrumenter hook before the agents starts. The hook enables passing
 * additional classes created by the agent to the static instrumenter's main.
 */
@AutoService(AgentListener.class)
public class AdditionalClassesInjectorInstaller implements AgentListener {

  @Override
  public void beforeAgent(
      Config config, AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    HelperInjector.setHelperInjectorListener(new AdditionalClassesInjector());
  }
}
