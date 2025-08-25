/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import opamp.proto.AgentToServer;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class CapabilitiesAppender implements AgentToServerAppender {
  private final Supplier<Long> capabilities;

  public static CapabilitiesAppender create(Supplier<Long> capabilities) {
    return new CapabilitiesAppender(capabilities);
  }

  private CapabilitiesAppender(Supplier<Long> capabilities) {
    this.capabilities = capabilities;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.capabilities(capabilities.get());
  }
}
