/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.io.Closeable;
import java.util.List;
import java.util.function.Consumer;

public interface PolicyProvider {
  /**
   * Retrieves the current list of telemetry policies provided by this source.
   *
   * @return A list of {@link TelemetryPolicy} objects.
   * @throws Exception if an error occurs while fetching the policies.
   */
  List<TelemetryPolicy> fetchPolicies() throws Exception;

  /**
   * Starts a mechanism to push policy updates to the provided consumer.
   *
   * <p>This method is optional and may be a no-op for providers that only support polling via
   * {@link #fetchPolicies()}.
   *
   * @param onUpdate A consumer that accepts the new list of policies when an update occurs.
   * @return A {@link Closeable} that stops watching when closed.
   */
  default Closeable startWatching(Consumer<List<TelemetryPolicy>> onUpdate) {
    return () -> {};
  }
}
