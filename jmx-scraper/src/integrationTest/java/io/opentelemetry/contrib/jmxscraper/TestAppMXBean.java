/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

@SuppressWarnings("unused")
public interface TestAppMXBean {

  int getIntValue();

  void stopApp();
}
