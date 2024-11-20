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
  "{producer}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName")},
   "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "ProducerCount",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.consumer.count",
  "The number of consumers currently reading from the broker.",
  "{consumer}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
  "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "ConsumerCount",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.memory.usage",
  "The percentage of configured memory used.",
  "%",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
   "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "MemoryPercentUsage",
  otel.&doubleValueCallback)

otel.instrument(activemqMetrics,
  "activemq.message.current",
  "The current number of messages waiting to be consumed.",
  "{message}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
  "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "QueueSize",
  otel.&longUpDownCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.expired",
  "The total number of messages not delivered because they expired.",
  "{message}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
  "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "ExpiredCount",
  otel.&longCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.enqueued",
  "The total number of messages received by the broker.",
  "{message}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
  "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "EnqueueCount",
  otel.&longCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.dequeued",
  "The total number of messages delivered to consumers.",
  "{message}",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
  "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "DequeueCount",
  otel.&longCounterCallback)

otel.instrument(activemqMetrics,
  "activemq.message.wait_time.avg",
  "The average time a message was held on a destination.",
  "ms",
  ["destination" : { mbean -> mbean.name().getKeyProperty("destinationName") },
  "broker"       : { mbean -> mbean.name().getKeyProperty("brokerName")}],
  "AverageEnqueueTime",
  otel.&doubleValueCallback)




def activemqMetricsNoDestination = otel.mbean(
    "org.apache.activemq:type=Broker,brokerName=*"
)

otel.instrument(activemqMetricsNoDestination,
  "activemq.connection.count",
  "The total number of current connections.",
  "{connection}",
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
