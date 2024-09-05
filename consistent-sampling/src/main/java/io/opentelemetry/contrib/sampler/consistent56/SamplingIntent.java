/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceState;

/** Interface for declaring sampling intent by Composable Samplers. */
@SuppressWarnings("CanIgnoreReturnValueSuggester")
public interface SamplingIntent {

  /**
   * Returns the suggested rejection threshold value. The returned value must be either from the
   * interval [0, 2^56) or be equal to ConsistentSamplingUtil.getInvalidThreshold().
   *
   * @return a threshold value
   */
  long getThreshold();

  /**
   * Returns a set of Attributes to be added to the Span in case of positive sampling decision.
   *
   * @return Attributes
   */
  default Attributes getAttributes() {
    return Attributes.empty();
  }

  /**
   * Given an input Tracestate and sampling Decision provide a Tracestate to be associated with the
   * Span.
   *
   * @param parentState the TraceState of the parent Span
   * @return a TraceState
   */
  default TraceState updateTraceState(TraceState parentState) {
    return parentState;
  }
}
