/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metrics;

import io.opentelemetry.ibm.mq.opentelemetry.ConfigWrapper;
import java.util.Map;

// This file is generated using weaver. Do not edit manually.

/** Configuration of metrics as defined in config.yml. */
public final class MetricsConfig {

  private final Map<String, ?> config;

  public MetricsConfig(ConfigWrapper config) {
    this.config = config.getMetrics();
  }

  public boolean isMqMessageRetryCountEnabled() {
    return isEnabled("mq.message.retry.count");
  }

  public boolean isMqStatusEnabled() {
    return isEnabled("mq.status");
  }

  public boolean isMqMaxSharingConversationsEnabled() {
    return isEnabled("mq.max.sharing.conversations");
  }

  public boolean isMqCurrentSharingConversationsEnabled() {
    return isEnabled("mq.current.sharing.conversations");
  }

  public boolean isMqByteReceivedEnabled() {
    return isEnabled("mq.byte.received");
  }

  public boolean isMqByteSentEnabled() {
    return isEnabled("mq.byte.sent");
  }

  public boolean isMqBuffersReceivedEnabled() {
    return isEnabled("mq.buffers.received");
  }

  public boolean isMqBuffersSentEnabled() {
    return isEnabled("mq.buffers.sent");
  }

  public boolean isMqMessageCountEnabled() {
    return isEnabled("mq.message.count");
  }

  public boolean isMqOpenInputCountEnabled() {
    return isEnabled("mq.open.input.count");
  }

  public boolean isMqOpenOutputCountEnabled() {
    return isEnabled("mq.open.output.count");
  }

  public boolean isMqHighQueueDepthEnabled() {
    return isEnabled("mq.high.queue.depth");
  }

  public boolean isMqServiceIntervalEnabled() {
    return isEnabled("mq.service.interval");
  }

  public boolean isMqQueueDepthFullEventEnabled() {
    return isEnabled("mq.queue.depth.full.event");
  }

  public boolean isMqQueueDepthHighEventEnabled() {
    return isEnabled("mq.queue.depth.high.event");
  }

  public boolean isMqQueueDepthLowEventEnabled() {
    return isEnabled("mq.queue.depth.low.event");
  }

  public boolean isMqUncommittedMessagesEnabled() {
    return isEnabled("mq.uncommitted.messages");
  }

  public boolean isMqOldestMsgAgeEnabled() {
    return isEnabled("mq.oldest.msg.age");
  }

  public boolean isMqCurrentMaxQueueFilesizeEnabled() {
    return isEnabled("mq.current.max.queue.filesize");
  }

  public boolean isMqCurrentQueueFilesizeEnabled() {
    return isEnabled("mq.current.queue.filesize");
  }

  public boolean isMqInstancesPerClientEnabled() {
    return isEnabled("mq.instances.per.client");
  }

  public boolean isMqMessageDeqCountEnabled() {
    return isEnabled("mq.message.deq.count");
  }

  public boolean isMqMessageEnqCountEnabled() {
    return isEnabled("mq.message.enq.count");
  }

  public boolean isMqQueueDepthEnabled() {
    return isEnabled("mq.queue.depth");
  }

  public boolean isMqServiceIntervalEventEnabled() {
    return isEnabled("mq.service.interval.event");
  }

  public boolean isMqReusableLogSizeEnabled() {
    return isEnabled("mq.reusable.log.size");
  }

  public boolean isMqManagerActiveChannelsEnabled() {
    return isEnabled("mq.manager.active.channels");
  }

  public boolean isMqRestartLogSizeEnabled() {
    return isEnabled("mq.restart.log.size");
  }

  public boolean isMqMaxQueueDepthEnabled() {
    return isEnabled("mq.max.queue.depth");
  }

  public boolean isMqOnqtime1Enabled() {
    return isEnabled("mq.onqtime.1");
  }

  public boolean isMqOnqtime2Enabled() {
    return isEnabled("mq.onqtime.2");
  }

  public boolean isMqMessageReceivedCountEnabled() {
    return isEnabled("mq.message.received.count");
  }

  public boolean isMqMessageSentCountEnabled() {
    return isEnabled("mq.message.sent.count");
  }

  public boolean isMqMaxInstancesEnabled() {
    return isEnabled("mq.max.instances");
  }

  public boolean isMqConnectionCountEnabled() {
    return isEnabled("mq.connection.count");
  }

  public boolean isMqManagerStatusEnabled() {
    return isEnabled("mq.manager.status");
  }

  public boolean isMqHeartbeatEnabled() {
    return isEnabled("mq.heartbeat");
  }

  public boolean isMqArchiveLogSizeEnabled() {
    return isEnabled("mq.archive.log.size");
  }

  public boolean isMqManagerMaxActiveChannelsEnabled() {
    return isEnabled("mq.manager.max.active.channels");
  }

  public boolean isMqManagerStatisticsIntervalEnabled() {
    return isEnabled("mq.manager.statistics.interval");
  }

  public boolean isMqPublishCountEnabled() {
    return isEnabled("mq.publish.count");
  }

  public boolean isMqSubscriptionCountEnabled() {
    return isEnabled("mq.subscription.count");
  }

  public boolean isMqListenerStatusEnabled() {
    return isEnabled("mq.listener.status");
  }

  public boolean isMqUnauthorizedEventEnabled() {
    return isEnabled("mq.unauthorized.event");
  }

  public boolean isMqManagerMaxHandlesEnabled() {
    return isEnabled("mq.manager.max.handles");
  }

  private boolean isEnabled(String key) {
    Object metricInfo = config.get(key);
    if (!(metricInfo instanceof Map)) {
      return false;
    }
    Object enabled = ((Map<?, ?>) metricInfo).get("enabled");
    if (enabled instanceof Boolean) {
      return (Boolean) enabled;
    }
    return false;
  }
}
