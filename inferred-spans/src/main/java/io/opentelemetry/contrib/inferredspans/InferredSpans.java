/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import java.time.Duration;
import javax.annotation.Nullable;

/**
 * A global accessor for the {@link InferredSpansProcessor} instance.
 *
 * <p>This class is for internal use only and may be removed in a future release.
 */
public final class InferredSpans {

  @Nullable private static volatile InferredSpansProcessor instance;

  private InferredSpans() {}

  /**
   * Sets the {@link InferredSpansProcessor} instance.
   *
   * @param processor the processor instance
   */
  public static void setInstance(@Nullable InferredSpansProcessor processor) {
    instance = processor;
  }

  /**
   * Returns whether inferred spans are enabled.
   *
   * @return whether inferred spans are enabled
   */
  public static boolean isEnabled() {
    return instance != null;
  }

  /**
   * Sets the profiler interval.
   *
   * @param interval the new profiler interval
   */
  public static void setProfilerInterval(Duration interval) {
    InferredSpansProcessor p = instance;
    if (p != null) {
      p.setProfilerInterval(interval);
    }
  }
}
