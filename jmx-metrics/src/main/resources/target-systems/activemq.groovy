/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


def activemqMetrics = otel.mbeans(
  [
    "org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*",
    "org.apache.activemq:type=Broker,brokerName=*,destinationType=Topic,destinationName=*"
  ]
)


otel.instrument(activemqMetrics,
  "activemq.producer.count",
  "The number of producers currently attached to the broker.",
  "{producers}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "ProducerCount",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.consumer.count",
  "The number of consumers currently reading from the broker.",
  "{consumers}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "ConsumerCount",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.memory.usage",
  "The percentage of configured memory used.",
  "%",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "MemoryPercentUsage",
  otel.&doubleValueCallback)

otel.instrument(activemqMetrics,
  "activemq.message.current",
  "The current number of messages waiting to be consumed.",
  "{messages}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "QueueSize",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.expired",
  "The total number of messages not delivered because they expired.",
  "{messages}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "ExpiredCount",
  otel.&longCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.enqueued",
  "The total number of messages received by the broker.",
  "{messages}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "EnqueueCount",
  otel.&longCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.dequeued",
  "The total number of messages delivered to consumers.",
  "{messages}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "DequeueCount",
  otel.&longCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.wait_time.avg",
  "The average time a message was held on a destination.",
  "ms",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") }],
  "AverageEnqueueTime",
  otel.&doubleValueCallback)




def activemqMetricsNoDestination = otel.mbean(
    "org.apache.activemq:type=Broker,brokerName=*"
)

otel.instrument(activemqMetricsNoDestination,
  "activemq.connection.count",
  "The total number of current connections.",
  "{connections}",
  "CurrentConnectionsCount",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetricsNoDestination,
  "activemq.disk.store_usage",
  "The percentage of configured disk used for persistent messages.",
  "%",
  "StorePercentUsage",
  otel.&doubleValueCallback)

otel.instrument(activemqMetricsNoDestination,
  "activemq.disk.temp_usage",
  "The percentage of configured disk used for non-persistent messages.",
  "%",
  "TempPercentUsage",
  otel.&doubleValueCallback)