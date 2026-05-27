/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.metrics;

import static io.opentelemetry.ibm.mq.metrics.MetricData.createMetricData;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

// This file is generated using weaver. Do not edit manually.

/** Metric definitions generated from a Weaver model. Do not edit manually. */
public final class MetricProducer implements io.opentelemetry.sdk.metrics.export.MetricProducer {
  private final Resource resource;
  private final InstrumentationScopeInfo instrumentationScopeInfo;
  private final BlockingQueue<MetricData> metricData;

  private final Map<Attributes, Long> counterIbmMqQueueDepthFullEvent;
  private final Map<Attributes, Long> counterIbmMqQueueDepthHighEvent;
  private final Map<Attributes, Long> counterIbmMqQueueDepthLowEvent;
  private final Map<Attributes, Long> counterIbmMqUnauthorizedEvent;
  private final Map<Attributes, Long> counterIbmMqQueueManagerUptime;
  private final Map<Attributes, Long> counterIbmMqConnectionErrors;

  private long currentEpochNanos;

  public MetricProducer(Resource resource, InstrumentationScopeInfo info) {
    this.resource = resource;
    this.instrumentationScopeInfo = info;
    this.metricData = new LinkedBlockingDeque<>();
    this.currentEpochNanos = Clock.getDefault().now();
    this.counterIbmMqQueueDepthFullEvent = new ConcurrentHashMap<>();
    this.counterIbmMqQueueDepthHighEvent = new ConcurrentHashMap<>();
    this.counterIbmMqQueueDepthLowEvent = new ConcurrentHashMap<>();
    this.counterIbmMqUnauthorizedEvent = new ConcurrentHashMap<>();
    this.counterIbmMqQueueManagerUptime = new ConcurrentHashMap<>();
    this.counterIbmMqConnectionErrors = new ConcurrentHashMap<>();
  }

  public void recordIbmMqMessageRetryCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.message.retry.count",
            "Number of message retries",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqStatus(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.status",
            "Channel status",
            "1",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMaxSharingConversations(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.max.sharing.conversations",
            "Maximum number of conversations permitted on this channel instance.",
            "{conversation}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqCurrentSharingConversations(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.current.sharing.conversations",
            "Current number of conversations permitted on this channel instance.",
            "{conversation}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqByteReceived(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.byte.received",
            "Number of bytes received",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqByteSent(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.byte.sent",
            "Number of bytes sent",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqBuffersReceived(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.buffers.received",
            "Buffers received",
            "{buffer}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqBuffersSent(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.buffers.sent",
            "Buffers sent",
            "{buffer}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMessageCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.message.count",
            "Message count",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqOpenInputCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.open.input.count",
            "Count of applications sending messages to the queue",
            "{application}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqOpenOutputCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.open.output.count",
            "Count of applications consuming messages from the queue",
            "{application}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqHighQueueDepth(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.high.queue.depth",
            "The current high queue depth",
            "{percent}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqServiceInterval(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.service.interval",
            "The queue service interval",
            "{percent}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void addIbmMqQueueDepthFullEvent(long value, Attributes attributes) {
    long cumulativeValue =
        this.counterIbmMqQueueDepthFullEvent.compute(
            attributes,
            (k, v) -> {
              if (v == null) {
                return value;
              } else {
                return v + value;
              }
            });
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.queue.depth.full.event",
            "The number of full queue events",
            "{event}",
            MetricDataType.LONG_SUM,
            SumData.createLongSumData(
                /* isMonotonic= */ true,
                AggregationTemporality.CUMULATIVE,
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos,
                        Clock.getDefault().now(),
                        attributes,
                        cumulativeValue)))));
  }

  public void addIbmMqQueueDepthHighEvent(long value, Attributes attributes) {
    long cumulativeValue =
        this.counterIbmMqQueueDepthHighEvent.compute(
            attributes,
            (k, v) -> {
              if (v == null) {
                return value;
              } else {
                return v + value;
              }
            });
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.queue.depth.high.event",
            "The number of high queue events",
            "{event}",
            MetricDataType.LONG_SUM,
            SumData.createLongSumData(
                /* isMonotonic= */ true,
                AggregationTemporality.CUMULATIVE,
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos,
                        Clock.getDefault().now(),
                        attributes,
                        cumulativeValue)))));
  }

  public void addIbmMqQueueDepthLowEvent(long value, Attributes attributes) {
    long cumulativeValue =
        this.counterIbmMqQueueDepthLowEvent.compute(
            attributes,
            (k, v) -> {
              if (v == null) {
                return value;
              } else {
                return v + value;
              }
            });
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.queue.depth.low.event",
            "The number of low queue events",
            "{event}",
            MetricDataType.LONG_SUM,
            SumData.createLongSumData(
                /* isMonotonic= */ true,
                AggregationTemporality.CUMULATIVE,
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos,
                        Clock.getDefault().now(),
                        attributes,
                        cumulativeValue)))));
  }

  public void recordIbmMqExpiredMessages(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.expired.messages",
            "Number of expired messages",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqUncommittedMessages(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.uncommitted.messages",
            "Number of uncommitted messages",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqOldestMsgAge(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.oldest.msg.age",
            "Queue message oldest age",
            "microseconds",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqCurrentMaxQueueFilesize(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.current.max.queue.filesize",
            "Current maximum queue file size",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqCurrentQueueFilesize(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.current.queue.filesize",
            "Current queue file size",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqInstancesPerClient(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.instances.per.client",
            "Instances per client",
            "{instance}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMessageDeqCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.message.deq.count",
            "Message dequeue count",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMessageEnqCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.message.enq.count",
            "Message enqueue count",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqQueueDepth(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.queue.depth",
            "Current queue depth",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqServiceIntervalEvent(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.service.interval.event",
            "Queue service interval event",
            "1",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqReusableLogSize(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.reusable.log.size",
            "The amount of space occupied, in megabytes, by log extents available to be reused.",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqManagerActiveChannels(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.manager.active.channels",
            "The queue manager active maximum channels limit",
            "{channel}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqRestartLogSize(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.restart.log.size",
            "Size of the log data required for restart recovery in megabytes.",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMaxQueueDepth(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.max.queue.depth",
            "Maximum queue depth",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqOnqtimeShortPeriod(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.onqtime.short_period",
            "Amount of time, in microseconds, that a message spent on the queue, over a short period",
            "microseconds",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqOnqtimeLongPeriod(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.onqtime.long_period",
            "Amount of time, in microseconds, that a message spent on the queue, over a longer period",
            "microseconds",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMessageReceivedCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.message.received.count",
            "Number of messages received",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMessageSentCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.message.sent.count",
            "Number of messages sent",
            "{message}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqMaxInstances(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.max.instances",
            "Max channel instances",
            "{instance}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqConnectionCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.connection.count",
            "Active connections count",
            "{connection}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqManagerStatus(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.manager.status",
            "Queue manager status",
            "1",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqHeartbeat(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.heartbeat",
            "Queue manager heartbeat",
            "1",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void addIbmMqQueueManagerUptime(long value, Attributes attributes) {
    long cumulativeValue =
        this.counterIbmMqQueueManagerUptime.compute(
            attributes,
            (k, v) -> {
              if (v == null) {
                return value;
              } else {
                return v + value;
              }
            });
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.queue_manager.uptime",
            "Queue manager uptime",
            "s",
            MetricDataType.LONG_SUM,
            SumData.createLongSumData(
                /* isMonotonic= */ true,
                AggregationTemporality.CUMULATIVE,
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos,
                        Clock.getDefault().now(),
                        attributes,
                        cumulativeValue)))));
  }

  public void recordIbmMqArchiveLogSize(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.archive.log.size",
            "Queue manager archive log size",
            "By",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqManagerMaxActiveChannels(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.manager.max.active.channels",
            "Queue manager max active channels",
            "{channel}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqManagerStatisticsInterval(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.manager.statistics.interval",
            "Queue manager statistics interval",
            "1",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqPublishCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.publish.count",
            "Topic publication count",
            "{publication}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqSubscriptionCount(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.subscription.count",
            "Topic subscription count",
            "{subscription}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void recordIbmMqListenerStatus(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.listener.status",
            "Listener status",
            "1",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void addIbmMqUnauthorizedEvent(long value, Attributes attributes) {
    long cumulativeValue =
        this.counterIbmMqUnauthorizedEvent.compute(
            attributes,
            (k, v) -> {
              if (v == null) {
                return value;
              } else {
                return v + value;
              }
            });
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.unauthorized.event",
            "Number of authentication error events",
            "{event}",
            MetricDataType.LONG_SUM,
            SumData.createLongSumData(
                /* isMonotonic= */ true,
                AggregationTemporality.CUMULATIVE,
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos,
                        Clock.getDefault().now(),
                        attributes,
                        cumulativeValue)))));
  }

  public void recordIbmMqManagerMaxHandles(long value, Attributes attributes) {
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.manager.max.handles",
            "Max open handles",
            "{event}",
            MetricDataType.LONG_GAUGE,
            GaugeData.createLongGaugeData(
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos, Clock.getDefault().now(), attributes, value)))));
  }

  public void addIbmMqConnectionErrors(long value, Attributes attributes) {
    long cumulativeValue =
        this.counterIbmMqConnectionErrors.compute(
            attributes,
            (k, v) -> {
              if (v == null) {
                return value;
              } else {
                return v + value;
              }
            });
    metricData.add(
        createMetricData(
            this.resource,
            this.instrumentationScopeInfo,
            "ibm.mq.connection.errors",
            "Number of connection errors",
            "{errors}",
            MetricDataType.LONG_SUM,
            SumData.createLongSumData(
                /* isMonotonic= */ true,
                AggregationTemporality.CUMULATIVE,
                Collections.singletonList(
                    LongPointData.create(
                        this.currentEpochNanos,
                        Clock.getDefault().now(),
                        attributes,
                        cumulativeValue)))));
  }

  @Override
  public List<MetricData> produce(Resource resource) {
    List<MetricData> collectedPoints = new ArrayList<>();
    this.metricData.drainTo(collectedPoints);
    this.currentEpochNanos = Clock.getDefault().now();
    return collectedPoints;
  }
}
