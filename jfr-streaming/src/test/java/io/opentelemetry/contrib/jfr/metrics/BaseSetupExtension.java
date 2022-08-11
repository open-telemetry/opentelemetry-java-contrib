/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class BaseSetupExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    // We need to use a unique key here, across all usages of this particular extension.
    String uniqueKey = this.getClass().getName();
    Object value = context.getRoot().getStore(GLOBAL).get(uniqueKey);
    if (value == null) {
      // First test container invocation.
      context.getRoot().getStore(GLOBAL).put(uniqueKey, this);
      setup();
    }
  }

  // Callback that is invoked <em>exactly once</em>
  // before the start of <em>all</em> test containers.
  abstract void setup();

  // Callback that is invoked <em>exactly once</em>
  // after the end of <em>all</em> test containers.
  // Inherited from {@code CloseableResource}
  @Override
  public abstract void close() throws Throwable;
}
