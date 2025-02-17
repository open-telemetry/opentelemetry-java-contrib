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

def producerMetrics = otel.mbeans("kafka.producer:client-id=*,type=producer-metrics")
otel.instrument(producerMetrics, "kafka.producer.io-wait-time-ns-avg",
        "The average length of time the I/O thread spent waiting for a socket ready for reads or writes", "ns",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "io-wait-time-ns-avg", otel.&doubleValueCallback)
otel.instrument(producerMetrics, "kafka.producer.outgoing-byte-rate",
        "The average number of outgoing bytes sent per second to all servers", "By",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "outgoing-byte-rate", otel.&doubleValueCallback)
otel.instrument(producerMetrics, "kafka.producer.request-latency-avg",
        "The average request latency", "ms",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "request-latency-avg", otel.&doubleValueCallback)
otel.instrument(producerMetrics, "kafka.producer.request-rate",
        "The average number of requests sent per second", "{request}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "request-rate", otel.&doubleValueCallback)
otel.instrument(producerMetrics, "kafka.producer.response-rate",
        "Responses received per second", "{response}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "response-rate", otel.&doubleValueCallback)

def producerTopicMetrics = otel.mbeans("kafka.producer:client-id=*,topic=*,type=producer-topic-metrics")
otel.instrument(producerTopicMetrics, "kafka.producer.byte-rate",
        "The average number of bytes sent per second for a topic", "By",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "byte-rate", otel.&doubleValueCallback)
otel.instrument(producerTopicMetrics, "kafka.producer.compression-rate",
        "The average compression rate of record batches for a topic", "{ratio}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "compression-rate", otel.&doubleValueCallback)
otel.instrument(producerTopicMetrics, "kafka.producer.record-error-rate",
        "The average per-second number of record sends that resulted in errors for a topic", "{record}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "record-error-rate", otel.&doubleValueCallback)
otel.instrument(producerTopicMetrics, "kafka.producer.record-retry-rate",
        "The average per-second number of retried record sends for a topic", "{record}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "record-retry-rate", otel.&doubleValueCallback)
otel.instrument(producerTopicMetrics, "kafka.producer.record-send-rate",
        "The average number of records sent per second for a topic", "{record}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "record-send-rate", otel.&doubleValueCallback)
