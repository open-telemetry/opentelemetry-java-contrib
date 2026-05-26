/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import opamp.proto.AgentToServer;
import opamp.proto.ComponentHealth;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class HealthAppender implements AgentToServerAppender {
  private final Supplier<ComponentHealth> health;

  public static HealthAppender create(Supplier<ComponentHealth> health) {
    return new HealthAppender(health);
  }

  private HealthAppender(Supplier<ComponentHealth> health) {
    this.health = health;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    ComponentHealth currentHealth = health.get();
    if (currentHealth != null) {
      builder.health(currentHealth);
    }
  }
}
