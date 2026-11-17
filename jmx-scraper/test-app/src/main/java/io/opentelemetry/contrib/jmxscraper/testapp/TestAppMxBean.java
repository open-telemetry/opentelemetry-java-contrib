/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.testapp;

import javax.management.MXBean;

/** MXBean interface exposed by {@link TestApp}. */
@MXBean
@SuppressWarnings("unused")
public interface TestAppMxBean {

  int getIntValue();

  void stopApp();
}
