/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.testapp;

import javax.management.MXBean;

@MXBean
@SuppressWarnings("unused")
public interface TestAppMxBean {

  int getIntValue();

  void stopApp();
}
