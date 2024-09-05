/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;

/** An interface for components to be used by composite consistent probability samplers. */
public interface ComposableSampler {

  /**
   * Returns the SamplingIntent that is used for the sampling decision. The SamplingIntent includes
   * the threshold value which will be used for the sampling decision.
   *
   * <p>NOTE: Keep in mind, that in any case the returned threshold value must not depend directly
   * or indirectly on the random value. In particular this means that the parent sampled flag must
   * not be used for the calculation of the threshold as the sampled flag depends itself on the
   * random value.
   */
  SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks);

  /** Return the string providing a description of the implementation. */
  String getDescription();
}
