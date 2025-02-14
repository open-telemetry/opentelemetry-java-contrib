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

def consumerFetchManagerMetrics = otel.mbeans("kafka.consumer:client-id=*,type=consumer-fetch-manager-metrics")
otel.instrument(consumerFetchManagerMetrics, "kafka.consumer.fetch-rate",
        "The number of fetch requests for all topics per second", "{request}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "fetch-rate", otel.&doubleValueCallback)

otel.instrument(consumerFetchManagerMetrics, "kafka.consumer.records-lag-max",
        "Number of messages the consumer lags behind the producer", "{record}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "records-lag-max", otel.&doubleValueCallback)

otel.instrument(consumerFetchManagerMetrics, "kafka.consumer.total.bytes-consumed-rate",
        "The average number of bytes consumed for all topics per second", "By",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "bytes-consumed-rate", otel.&doubleValueCallback)

otel.instrument(consumerFetchManagerMetrics, "kafka.consumer.total.fetch-size-avg",
        "The average number of bytes fetched per request for all topics", "By",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "fetch-size-avg", otel.&doubleValueCallback)

otel.instrument(consumerFetchManagerMetrics, "kafka.consumer.total.records-consumed-rate",
        "The average number of records consumed for all topics per second", "{record}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") }],
        "records-consumed-rate", otel.&doubleValueCallback)

def consumerFetchManagerMetricsByTopic = otel.mbeans("kafka.consumer:client-id=*,topic=*,type=consumer-fetch-manager-metrics")
otel.instrument(consumerFetchManagerMetricsByTopic, "kafka.consumer.bytes-consumed-rate",
        "The average number of bytes consumed per second", "By",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "bytes-consumed-rate", otel.&doubleValueCallback)

otel.instrument(consumerFetchManagerMetricsByTopic, "kafka.consumer.fetch-size-avg",
        "The average number of bytes fetched per request", "By",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "fetch-size-avg", otel.&doubleValueCallback)

otel.instrument(consumerFetchManagerMetricsByTopic, "kafka.consumer.records-consumed-rate",
        "The average number of records consumed per second", "{record}",
        ["client-id" : { mbean -> mbean.name().getKeyProperty("client-id") },
            "topic" : { mbean -> mbean.name().getKeyProperty("topic") }],
        "records-consumed-rate", otel.&doubleValueCallback)
