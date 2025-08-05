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

  public boolean isIbmMqMessageRetryCountEnabled() {
    return isEnabled("ibm.mq.message.retry.count");
  }

  public boolean isIbmMqStatusEnabled() {
    return isEnabled("ibm.mq.status");
  }

  public boolean isIbmMqMaxSharingConversationsEnabled() {
    return isEnabled("ibm.mq.max.sharing.conversations");
  }

  public boolean isIbmMqCurrentSharingConversationsEnabled() {
    return isEnabled("ibm.mq.current.sharing.conversations");
  }

  public boolean isIbmMqByteReceivedEnabled() {
    return isEnabled("ibm.mq.byte.received");
  }

  public boolean isIbmMqByteSentEnabled() {
    return isEnabled("ibm.mq.byte.sent");
  }

  public boolean isIbmMqBuffersReceivedEnabled() {
    return isEnabled("ibm.mq.buffers.received");
  }

  public boolean isIbmMqBuffersSentEnabled() {
    return isEnabled("ibm.mq.buffers.sent");
  }

  public boolean isIbmMqMessageCountEnabled() {
    return isEnabled("ibm.mq.message.count");
  }

  public boolean isIbmMqOpenInputCountEnabled() {
    return isEnabled("ibm.mq.open.input.count");
  }

  public boolean isIbmMqOpenOutputCountEnabled() {
    return isEnabled("ibm.mq.open.output.count");
  }

  public boolean isIbmMqHighQueueDepthEnabled() {
    return isEnabled("ibm.mq.high.queue.depth");
  }

  public boolean isIbmMqServiceIntervalEnabled() {
    return isEnabled("ibm.mq.service.interval");
  }

  public boolean isIbmMqQueueDepthFullEventEnabled() {
    return isEnabled("ibm.mq.queue.depth.full.event");
  }

  public boolean isIbmMqQueueDepthHighEventEnabled() {
    return isEnabled("ibm.mq.queue.depth.high.event");
  }

  public boolean isIbmMqQueueDepthLowEventEnabled() {
    return isEnabled("ibm.mq.queue.depth.low.event");
  }

  public boolean isIbmMqUncommittedMessagesEnabled() {
    return isEnabled("ibm.mq.uncommitted.messages");
  }

  public boolean isIbmMqOldestMsgAgeEnabled() {
    return isEnabled("ibm.mq.oldest.msg.age");
  }

  public boolean isIbmMqCurrentMaxQueueFilesizeEnabled() {
    return isEnabled("ibm.mq.current.max.queue.filesize");
  }

  public boolean isIbmMqCurrentQueueFilesizeEnabled() {
    return isEnabled("ibm.mq.current.queue.filesize");
  }

  public boolean isIbmMqInstancesPerClientEnabled() {
    return isEnabled("ibm.mq.instances.per.client");
  }

  public boolean isIbmMqMessageDeqCountEnabled() {
    return isEnabled("ibm.mq.message.deq.count");
  }

  public boolean isIbmMqMessageEnqCountEnabled() {
    return isEnabled("ibm.mq.message.enq.count");
  }

  public boolean isIbmMqQueueDepthEnabled() {
    return isEnabled("ibm.mq.queue.depth");
  }

  public boolean isIbmMqServiceIntervalEventEnabled() {
    return isEnabled("ibm.mq.service.interval.event");
  }

  public boolean isIbmMqReusableLogSizeEnabled() {
    return isEnabled("ibm.mq.reusable.log.size");
  }

  public boolean isIbmMqManagerActiveChannelsEnabled() {
    return isEnabled("ibm.mq.manager.active.channels");
  }

  public boolean isIbmMqRestartLogSizeEnabled() {
    return isEnabled("ibm.mq.restart.log.size");
  }

  public boolean isIbmMqMaxQueueDepthEnabled() {
    return isEnabled("ibm.mq.max.queue.depth");
  }

  public boolean isIbmMqOnqtimeShortPeriodEnabled() {
    return isEnabled("ibm.mq.onqtime.short_period");
  }

  public boolean isIbmMqOnqtimeLongPeriodEnabled() {
    return isEnabled("ibm.mq.onqtime.long_period");
  }

  public boolean isIbmMqMessageReceivedCountEnabled() {
    return isEnabled("ibm.mq.message.received.count");
  }

  public boolean isIbmMqMessageSentCountEnabled() {
    return isEnabled("ibm.mq.message.sent.count");
  }

  public boolean isIbmMqMaxInstancesEnabled() {
    return isEnabled("ibm.mq.max.instances");
  }

  public boolean isIbmMqConnectionCountEnabled() {
    return isEnabled("ibm.mq.connection.count");
  }

  public boolean isIbmMqManagerStatusEnabled() {
    return isEnabled("ibm.mq.manager.status");
  }

  public boolean isIbmMqHeartbeatEnabled() {
    return isEnabled("ibm.mq.heartbeat");
  }

  public boolean isIbmMqArchiveLogSizeEnabled() {
    return isEnabled("ibm.mq.archive.log.size");
  }

  public boolean isIbmMqManagerMaxActiveChannelsEnabled() {
    return isEnabled("ibm.mq.manager.max.active.channels");
  }

  public boolean isIbmMqManagerStatisticsIntervalEnabled() {
    return isEnabled("ibm.mq.manager.statistics.interval");
  }

  public boolean isIbmMqPublishCountEnabled() {
    return isEnabled("ibm.mq.publish.count");
  }

  public boolean isIbmMqSubscriptionCountEnabled() {
    return isEnabled("ibm.mq.subscription.count");
  }

  public boolean isIbmMqListenerStatusEnabled() {
    return isEnabled("ibm.mq.listener.status");
  }

  public boolean isIbmMqUnauthorizedEventEnabled() {
    return isEnabled("ibm.mq.unauthorized.event");
  }

  public boolean isIbmMqManagerMaxHandlesEnabled() {
    return isEnabled("ibm.mq.manager.max.handles");
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
