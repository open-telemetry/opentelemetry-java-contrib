/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ConsistentAlwaysOnSampler extends ConsistentSampler {

  @Override
  protected int getP(int parentP, boolean isRoot) {
    return 0;
  }

  @Override
  public String getDescription() {
    return "ConsistentAlwaysOnSampler";
  }
}
