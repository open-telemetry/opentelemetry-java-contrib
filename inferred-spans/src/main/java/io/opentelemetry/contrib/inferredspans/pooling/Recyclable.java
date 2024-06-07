/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.pooling;

public interface Recyclable {

  /** resets pooled object state so it can be reused */
  void resetState();
}
