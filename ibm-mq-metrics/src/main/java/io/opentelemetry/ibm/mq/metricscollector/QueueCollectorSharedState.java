/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metricscollector;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

final class QueueCollectorSharedState {

  private final ConcurrentHashMap<String, String> queueNameToType = new ConcurrentHashMap<>();

  QueueCollectorSharedState() {}

  public void putQueueType(String name, String value) {
    queueNameToType.put(name, value);
  }

  @Nullable
  public String getType(String name) {
    return queueNameToType.get(name);
  }
}
