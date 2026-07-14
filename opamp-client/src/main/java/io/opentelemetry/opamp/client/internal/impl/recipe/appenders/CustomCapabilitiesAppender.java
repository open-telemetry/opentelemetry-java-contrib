/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import opamp.proto.AgentToServer;
import opamp.proto.CustomCapabilities;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class CustomCapabilitiesAppender implements AgentToServerAppender {
  private final Supplier<CustomCapabilities> customCapabilities;

  public static CustomCapabilitiesAppender create(Supplier<CustomCapabilities> customCapabilities) {
    return new CustomCapabilitiesAppender(customCapabilities);
  }

  private CustomCapabilitiesAppender(Supplier<CustomCapabilities> customCapabilities) {
    this.customCapabilities = customCapabilities;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.custom_capabilities(customCapabilities.get());
  }
}
