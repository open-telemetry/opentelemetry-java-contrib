/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metrics;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import java.util.function.Function;

// This file is generated using weaver. Do not edit manually.

/** Metric definitions generated from a Weaver model. Do not edit manually. */
public final class Metrics {
  public static final Function<Integer, Long> MIBY_TO_BYTES = x -> x * 1024L * 1024L;

  private Metrics() {}

  public static LongGauge createIbmMqMessageRetryCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.message.retry.count")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Number of message retries")
        .build();
  }

  public static LongGauge createIbmMqStatus(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.status")
        .ofLongs()
        .setUnit("1")
        .setDescription("Channel status")
        .build();
  }

  public static LongGauge createIbmMqMaxSharingConversations(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.max.sharing.conversations")
        .ofLongs()
        .setUnit("{conversation}")
        .setDescription("Maximum number of conversations permitted on this channel instance.")
        .build();
  }

  public static LongGauge createIbmMqCurrentSharingConversations(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.current.sharing.conversations")
        .ofLongs()
        .setUnit("{conversation}")
        .setDescription("Current number of conversations permitted on this channel instance.")
        .build();
  }

  public static LongGauge createIbmMqByteReceived(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.byte.received")
        .ofLongs()
        .setUnit("By")
        .setDescription("Number of bytes received")
        .build();
  }

  public static LongGauge createIbmMqByteSent(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.byte.sent")
        .ofLongs()
        .setUnit("By")
        .setDescription("Number of bytes sent")
        .build();
  }

  public static LongGauge createIbmMqBuffersReceived(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.buffers.received")
        .ofLongs()
        .setUnit("{buffer}")
        .setDescription("Buffers received")
        .build();
  }

  public static LongGauge createIbmMqBuffersSent(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.buffers.sent")
        .ofLongs()
        .setUnit("{buffer}")
        .setDescription("Buffers sent")
        .build();
  }

  public static LongGauge createIbmMqMessageCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.message.count")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Message count")
        .build();
  }

  public static LongGauge createIbmMqOpenInputCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.open.input.count")
        .ofLongs()
        .setUnit("{application}")
        .setDescription("Count of applications sending messages to the queue")
        .build();
  }

  public static LongGauge createIbmMqOpenOutputCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.open.output.count")
        .ofLongs()
        .setUnit("{application}")
        .setDescription("Count of applications consuming messages from the queue")
        .build();
  }

  public static LongGauge createIbmMqHighQueueDepth(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.high.queue.depth")
        .ofLongs()
        .setUnit("{percent}")
        .setDescription("The current high queue depth")
        .build();
  }

  public static LongGauge createIbmMqServiceInterval(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.service.interval")
        .ofLongs()
        .setUnit("{percent}")
        .setDescription("The queue service interval")
        .build();
  }

  public static LongCounter createIbmMqQueueDepthFullEvent(Meter meter) {
    return meter
        .counterBuilder("ibm.mq.queue.depth.full.event")
        .setUnit("{event}")
        .setDescription("The number of full queue events")
        .build();
  }

  public static LongCounter createIbmMqQueueDepthHighEvent(Meter meter) {
    return meter
        .counterBuilder("ibm.mq.queue.depth.high.event")
        .setUnit("{event}")
        .setDescription("The number of high queue events")
        .build();
  }

  public static LongCounter createIbmMqQueueDepthLowEvent(Meter meter) {
    return meter
        .counterBuilder("ibm.mq.queue.depth.low.event")
        .setUnit("{event}")
        .setDescription("The number of low queue events")
        .build();
  }

  public static LongGauge createIbmMqUncommittedMessages(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.uncommitted.messages")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Number of uncommitted messages")
        .build();
  }

  public static LongGauge createIbmMqOldestMsgAge(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.oldest.msg.age")
        .ofLongs()
        .setUnit("microseconds")
        .setDescription("Queue message oldest age")
        .build();
  }

  public static LongGauge createIbmMqCurrentMaxQueueFilesize(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.current.max.queue.filesize")
        .ofLongs()
        .setUnit("By")
        .setDescription("Current maximum queue file size")
        .build();
  }

  public static LongGauge createIbmMqCurrentQueueFilesize(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.current.queue.filesize")
        .ofLongs()
        .setUnit("By")
        .setDescription("Current queue file size")
        .build();
  }

  public static LongGauge createIbmMqInstancesPerClient(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.instances.per.client")
        .ofLongs()
        .setUnit("{instance}")
        .setDescription("Instances per client")
        .build();
  }

  public static LongGauge createIbmMqMessageDeqCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.message.deq.count")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Message dequeue count")
        .build();
  }

  public static LongGauge createIbmMqMessageEnqCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.message.enq.count")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Message enqueue count")
        .build();
  }

  public static LongGauge createIbmMqQueueDepth(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.queue.depth")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Current queue depth")
        .build();
  }

  public static LongGauge createIbmMqServiceIntervalEvent(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.service.interval.event")
        .ofLongs()
        .setUnit("1")
        .setDescription("Queue service interval event")
        .build();
  }

  public static LongGauge createIbmMqReusableLogSize(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.reusable.log.size")
        .ofLongs()
        .setUnit("By")
        .setDescription(
            "The amount of space occupied, in megabytes, by log extents available to be reused.")
        .build();
  }

  public static LongGauge createIbmMqManagerActiveChannels(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.manager.active.channels")
        .ofLongs()
        .setUnit("{channel}")
        .setDescription("The queue manager active maximum channels limit")
        .build();
  }

  public static LongGauge createIbmMqRestartLogSize(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.restart.log.size")
        .ofLongs()
        .setUnit("By")
        .setDescription("Size of the log data required for restart recovery in megabytes.")
        .build();
  }

  public static LongGauge createIbmMqMaxQueueDepth(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.max.queue.depth")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Maximum queue depth")
        .build();
  }

  public static LongGauge createIbmMqOnqtime1(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.onqtime.1")
        .ofLongs()
        .setUnit("microseconds")
        .setDescription(
            "Amount of time, in microseconds, that a message spent on the queue, over a short period")
        .build();
  }

  public static LongGauge createIbmMqOnqtime2(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.onqtime.2")
        .ofLongs()
        .setUnit("microseconds")
        .setDescription(
            "Amount of time, in microseconds, that a message spent on the queue, over a longer period")
        .build();
  }

  public static LongGauge createIbmMqMessageReceivedCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.message.received.count")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Number of messages received")
        .build();
  }

  public static LongGauge createIbmMqMessageSentCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.message.sent.count")
        .ofLongs()
        .setUnit("{message}")
        .setDescription("Number of messages sent")
        .build();
  }

  public static LongGauge createIbmMqMaxInstances(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.max.instances")
        .ofLongs()
        .setUnit("{instance}")
        .setDescription("Max channel instances")
        .build();
  }

  public static LongGauge createIbmMqConnectionCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.connection.count")
        .ofLongs()
        .setUnit("{connection}")
        .setDescription("Active connections count")
        .build();
  }

  public static LongGauge createIbmMqManagerStatus(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.manager.status")
        .ofLongs()
        .setUnit("1")
        .setDescription("Queue manager status")
        .build();
  }

  public static LongGauge createIbmMqHeartbeat(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.heartbeat")
        .ofLongs()
        .setUnit("1")
        .setDescription("Queue manager heartbeat")
        .build();
  }

  public static LongGauge createIbmMqArchiveLogSize(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.archive.log.size")
        .ofLongs()
        .setUnit("By")
        .setDescription("Queue manager archive log size")
        .build();
  }

  public static LongGauge createIbmMqManagerMaxActiveChannels(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.manager.max.active.channels")
        .ofLongs()
        .setUnit("{channel}")
        .setDescription("Queue manager max active channels")
        .build();
  }

  public static LongGauge createIbmMqManagerStatisticsInterval(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.manager.statistics.interval")
        .ofLongs()
        .setUnit("1")
        .setDescription("Queue manager statistics interval")
        .build();
  }

  public static LongGauge createIbmMqPublishCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.publish.count")
        .ofLongs()
        .setUnit("{publication}")
        .setDescription("Topic publication count")
        .build();
  }

  public static LongGauge createIbmMqSubscriptionCount(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.subscription.count")
        .ofLongs()
        .setUnit("{subscription}")
        .setDescription("Topic subscription count")
        .build();
  }

  public static LongGauge createIbmMqListenerStatus(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.listener.status")
        .ofLongs()
        .setUnit("1")
        .setDescription("Listener status")
        .build();
  }

  public static LongCounter createIbmMqUnauthorizedEvent(Meter meter) {
    return meter
        .counterBuilder("ibm.mq.unauthorized.event")
        .setUnit("{event}")
        .setDescription("Number of authentication error events")
        .build();
  }

  public static LongGauge createIbmMqManagerMaxHandles(Meter meter) {
    return meter
        .gaugeBuilder("ibm.mq.manager.max.handles")
        .ofLongs()
        .setUnit("{event}")
        .setDescription("Max open handles")
        .build();
  }
}
