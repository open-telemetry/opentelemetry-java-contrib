/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.util.Collections;
import java.util.List;

/**
 * This class is just a factory that provides a ServiceNameDetector that knows how to find and parse
 * the most common application server configuration files.
 */
final class CommonAppServersServiceNameDetector {

  static ServiceNameDetector create() {
    return new DelegatingServiceNameDetector(detectors());
  }

  private CommonAppServersServiceNameDetector() {}

  private static List<ServiceNameDetector> detectors() {
    // TBD: This will contain common app server detector implementations
    return Collections.emptyList();
  }
}
