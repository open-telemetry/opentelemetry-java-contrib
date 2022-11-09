/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;

final class DelegatingServiceNameDetector implements ServiceNameDetector {

  private static final Logger logger =
      Logger.getLogger(DelegatingServiceNameDetector.class.getName());

  private final List<ServiceNameDetector> delegates;

  DelegatingServiceNameDetector(List<ServiceNameDetector> delegates) {
    this.delegates = Collections.unmodifiableList(new ArrayList<>(delegates));
  }

  @Override
  @Nullable
  public String detect() {
    for (ServiceNameDetector detector : delegates) {
      try {
        String name = detector.detect();
        if (name != null) {
          return name;
        }
      } catch (Exception exception) {
        if (logger.isLoggable(FINE)) {
          logger.log(
              FINE,
              "Service name detector '" + detector.getClass().getSimpleName() + "' failed with",
              exception);
        }
      }
    }

    return null;
  }
}
