/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.pooling;

/**
 * An object whose state can be reset so it can be reused from a pool.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface Recyclable {

  /** resets pooled object state so it can be reused */
  void resetState();
}
