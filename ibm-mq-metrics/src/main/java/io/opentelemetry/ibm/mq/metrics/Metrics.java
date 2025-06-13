/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.ibm.mq.metrics;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;

// This file is generated using weaver. Do not edit manually.

/** Metric definitions generated from a Weaver model. Do not edit manually. */
public final class Metrics {

  public static LongGauge createMqMessageRetryCount(Meter meter) {
    return meter.gaugeBuilder("mq.message.retry.count").ofLongs().setUnit("{messages}").setDescription("Number of message retries").build();
  }

  public static LongGauge createMqStatus(Meter meter) {
    return meter.gaugeBuilder("mq.status").ofLongs().setUnit("1").setDescription("Channel status").build();
  }

  public static LongGauge createMqMaxSharingConversations(Meter meter) {
    return meter.gaugeBuilder("mq.max.sharing.conversations").ofLongs().setUnit("{conversations}").setDescription("Maximum number of conversations permitted on this channel instance.").build();
  }

  public static LongGauge createMqCurrentSharingConversations(Meter meter) {
    return meter.gaugeBuilder("mq.current.sharing.conversations").ofLongs().setUnit("{conversations}").setDescription("Current number of conversations permitted on this channel instance.").build();
  }

  public static LongGauge createMqByteReceived(Meter meter) {
    return meter.gaugeBuilder("mq.byte.received").ofLongs().setUnit("{bytes}").setDescription("Number of bytes received").build();
  }

  public static LongGauge createMqByteSent(Meter meter) {
    return meter.gaugeBuilder("mq.byte.sent").ofLongs().setUnit("{bytes}").setDescription("Number of bytes sent").build();
  }

  public static LongGauge createMqBuffersReceived(Meter meter) {
    return meter.gaugeBuilder("mq.buffers.received").ofLongs().setUnit("{buffers}").setDescription("Buffers received").build();
  }

  public static LongGauge createMqBuffersSent(Meter meter) {
    return meter.gaugeBuilder("mq.buffers.sent").ofLongs().setUnit("{buffers}").setDescription("Buffers sent").build();
  }

  public static LongGauge createMqMessageCount(Meter meter) {
    return meter.gaugeBuilder("mq.message.count").ofLongs().setUnit("{messages}").setDescription("Message count").build();
  }

  public static LongGauge createMqOpenInputCount(Meter meter) {
    return meter.gaugeBuilder("mq.open.input.count").ofLongs().setUnit("{applications}").setDescription("Count of applications sending messages to the queue").build();
  }

  public static LongGauge createMqOpenOutputCount(Meter meter) {
    return meter.gaugeBuilder("mq.open.output.count").ofLongs().setUnit("{applications}").setDescription("Count of applications consuming messages from the queue").build();
  }

  public static LongGauge createMqHighQueueDepth(Meter meter) {
    return meter.gaugeBuilder("mq.high.queue.depth").ofLongs().setUnit("{percent}").setDescription("The current high queue depth").build();
  }

  public static LongGauge createMqServiceInterval(Meter meter) {
    return meter.gaugeBuilder("mq.service.interval").ofLongs().setUnit("{percent}").setDescription("The queue service interval").build();
  }

  public static LongCounter createMqQueueDepthFullEvent(Meter meter) {
    return meter.counterBuilder("mq.queue.depth.full.event").setUnit("{events}").setDescription("The number of full queue events").build();
  }

  public static LongCounter createMqQueueDepthHighEvent(Meter meter) {
    return meter.counterBuilder("mq.queue.depth.high.event").setUnit("{events}").setDescription("The number of high queue events").build();
  }

  public static LongCounter createMqQueueDepthLowEvent(Meter meter) {
    return meter.counterBuilder("mq.queue.depth.low.event").setUnit("{events}").setDescription("The number of low queue events").build();
  }

  public static LongGauge createMqUncommittedMessages(Meter meter) {
    return meter.gaugeBuilder("mq.uncommitted.messages").ofLongs().setUnit("{messages}").setDescription("Number of uncommitted messages").build();
  }

  public static LongGauge createMqOldestMsgAge(Meter meter) {
    return meter.gaugeBuilder("mq.oldest.msg.age").ofLongs().setUnit("microseconds").setDescription("Queue message oldest age").build();
  }

  public static LongGauge createMqCurrentMaxQueueFilesize(Meter meter) {
    return meter.gaugeBuilder("mq.current.max.queue.filesize").ofLongs().setUnit("mib").setDescription("Current maximum queue file size").build();
  }

  public static LongGauge createMqCurrentQueueFilesize(Meter meter) {
    return meter.gaugeBuilder("mq.current.queue.filesize").ofLongs().setUnit("mib").setDescription("Current queue file size").build();
  }

  public static LongGauge createMqInstancesPerClient(Meter meter) {
    return meter.gaugeBuilder("mq.instances.per.client").ofLongs().setUnit("{instances}").setDescription("Instances per client").build();
  }

  public static LongGauge createMqMessageDeqCount(Meter meter) {
    return meter.gaugeBuilder("mq.message.deq.count").ofLongs().setUnit("{messages}").setDescription("Message dequeue count").build();
  }

  public static LongGauge createMqMessageEnqCount(Meter meter) {
    return meter.gaugeBuilder("mq.message.enq.count").ofLongs().setUnit("{messages}").setDescription("Message enqueue count").build();
  }

  public static LongGauge createMqQueueDepth(Meter meter) {
    return meter.gaugeBuilder("mq.queue.depth").ofLongs().setUnit("{messages}").setDescription("Current queue depth").build();
  }

  public static LongGauge createMqServiceIntervalEvent(Meter meter) {
    return meter.gaugeBuilder("mq.service.interval.event").ofLongs().setUnit("1").setDescription("Queue service interval event").build();
  }

  public static LongGauge createMqReusableLogSize(Meter meter) {
    return meter.gaugeBuilder("mq.reusable.log.size").ofLongs().setUnit("mib").setDescription("The amount of space occupied, in megabytes, by log extents available to be reused.").build();
  }

  public static LongGauge createMqManagerActiveChannels(Meter meter) {
    return meter.gaugeBuilder("mq.manager.active.channels").ofLongs().setUnit("{channels}").setDescription("The queue manager active maximum channels limit").build();
  }

  public static LongGauge createMqRestartLogSize(Meter meter) {
    return meter.gaugeBuilder("mq.restart.log.size").ofLongs().setUnit("mib").setDescription("Size of the log data required for restart recovery in megabytes.").build();
  }

  public static LongGauge createMqMaxQueueDepth(Meter meter) {
    return meter.gaugeBuilder("mq.max.queue.depth").ofLongs().setUnit("{messages}").setDescription("Maximum queue depth").build();
  }

  public static LongGauge createMqOnqtime1(Meter meter) {
    return meter.gaugeBuilder("mq.onqtime.1").ofLongs().setUnit("microseconds").setDescription("Amount of time, in microseconds, that a message spent on the queue, over a short period").build();
  }

  public static LongGauge createMqOnqtime2(Meter meter) {
    return meter.gaugeBuilder("mq.onqtime.2").ofLongs().setUnit("microseconds").setDescription("Amount of time, in microseconds, that a message spent on the queue, over a longer period").build();
  }

  public static LongGauge createMqMessageReceivedCount(Meter meter) {
    return meter.gaugeBuilder("mq.message.received.count").ofLongs().setUnit("{messages}").setDescription("Number of messages received").build();
  }

  public static LongGauge createMqMessageSentCount(Meter meter) {
    return meter.gaugeBuilder("mq.message.sent.count").ofLongs().setUnit("{messages}").setDescription("Number of messages sent").build();
  }

  public static LongGauge createMqMaxInstances(Meter meter) {
    return meter.gaugeBuilder("mq.max.instances").ofLongs().setUnit("{instances}").setDescription("Max channel instances").build();
  }

  public static LongGauge createMqConnectionCount(Meter meter) {
    return meter.gaugeBuilder("mq.connection.count").ofLongs().setUnit("{connections}").setDescription("Active connections count").build();
  }

  public static LongGauge createMqManagerStatus(Meter meter) {
    return meter.gaugeBuilder("mq.manager.status").ofLongs().setUnit("1").setDescription("Queue manager status").build();
  }

  public static LongGauge createMqHeartbeat(Meter meter) {
    return meter.gaugeBuilder("mq.heartbeat").ofLongs().setUnit("1").setDescription("Queue manager heartbeat").build();
  }

  public static LongGauge createMqArchiveLogSize(Meter meter) {
    return meter.gaugeBuilder("mq.archive.log.size").ofLongs().setUnit("mib").setDescription("Queue manager archive log size").build();
  }

  public static LongGauge createMqManagerMaxActiveChannels(Meter meter) {
    return meter.gaugeBuilder("mq.manager.max.active.channels").ofLongs().setUnit("{channels}").setDescription("Queue manager max active channels").build();
  }

  public static LongGauge createMqManagerStatisticsInterval(Meter meter) {
    return meter.gaugeBuilder("mq.manager.statistics.interval").ofLongs().setUnit("1").setDescription("Queue manager statistics interval").build();
  }

  public static LongGauge createMqPublishCount(Meter meter) {
    return meter.gaugeBuilder("mq.publish.count").ofLongs().setUnit("{publications}").setDescription("Topic publication count").build();
  }

  public static LongGauge createMqSubscriptionCount(Meter meter) {
    return meter.gaugeBuilder("mq.subscription.count").ofLongs().setUnit("{subscriptions}").setDescription("Topic subscription count").build();
  }

  public static LongGauge createMqListenerStatus(Meter meter) {
    return meter.gaugeBuilder("mq.listener.status").ofLongs().setUnit("1").setDescription("Listener status").build();
  }

  public static LongCounter createMqUnauthorizedEvent(Meter meter) {
    return meter.counterBuilder("mq.unauthorized.event").setUnit("{events}").setDescription("Number of authentication error events").build();
  }

  public static LongGauge createMqManagerMaxHandles(Meter meter) {
    return meter.gaugeBuilder("mq.manager.max.handles").ofLongs().setUnit("{events}").setDescription("Max open handles").build();
  }

}