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

package io.opentelemetry.contrib.jmxmetrics

import io.opentelemetry.proto.common.v1.StringKeyValue
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Retry
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(90)
@Retry
class KafkaProducerTargetSystemIntegrationTests extends OtlpIntegrationTest {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use Kafka as target system'
        targets = ["kafka-producer"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/kafka-producer.properties',  otlpPort, 0, false)

        expect:
        when: 'we receive metrics from the JMX metric gatherer'
        List<ResourceMetrics> receivedMetrics = collector.receivedMetrics
        then: 'they are of the expected size'
        receivedMetrics.size() == 1

        when: "we examine the received metric's instrumentation library metrics lists"
        ResourceMetrics receivedMetric = receivedMetrics.get(0)
        List<InstrumentationLibraryMetrics> ilMetrics =
                receivedMetric.instrumentationLibraryMetricsList
        then: 'they of the expected size'
        ilMetrics.size() == 1

        when: 'we examine the instrumentation library metric metrics list'
        InstrumentationLibraryMetrics ilMetric = ilMetrics.get(0)
        ArrayList<Metric> metrics = ilMetric.metricsList as ArrayList
        metrics.sort{ a, b -> a.name <=> b.name}
        then: 'they are of the expected size and content'
        metrics.size() == 10

        [
            [
                "kafka.producer.byte-rate",
                "The average number of bytes sent per second for a topic",
                "by",
                ['client-id' : '', 'topic' : ['test-topic-1']],
            ],
            [
                "kafka.producer.compression-rate",
                "The average compression rate of record batches for a topic",
                "1",
                ['client-id' : '', 'topic' : ['test-topic-1']],
            ],
            [
                "kafka.producer.io-wait-time-ns-avg",
                "The average length of time the I/O thread spent waiting for a socket ready for reads or writes",
                "ns",
                ['client-id' : ''],
            ],
            [
                "kafka.producer.outgoing-byte-rate",
                "The average number of outgoing bytes sent per second to all servers",
                "by",
                ['client-id' : ''],
            ],
            [
                "kafka.producer.record-error-rate",
                "The average per-second number of record sends that resulted in errors for a topic",
                "1",
                ['client-id' : '', 'topic' : ['test-topic-1']],
            ],
            [
                "kafka.producer.record-retry-rate",
                "The average per-second number of retried record sends for a topic",
                "1",
                ['client-id' : '', 'topic' : ['test-topic-1']],
            ],
            [
                "kafka.producer.record-send-rate",
                "The average number of records sent per second for a topic",
                "1",
                ['client-id' : '', 'topic' : ['test-topic-1']],
            ],
            [
                "kafka.producer.request-latency-avg",
                "The average request latency",
                "ms",
                ['client-id' : ''],
            ],
            [
                "kafka.producer.request-rate",
                "The average number of requests sent per second",
                "1",
                ['client-id' : ''],
            ],
            [
                "kafka.producer.response-rate",
                "Responses received per second",
                "1",
                ['client-id' : ''],
            ],
        ].eachWithIndex{ item, index ->
            Metric metric = metrics.get(index)
            assert metric.name == item[0]
            assert metric.description == item[1]
            assert metric.unit == item[2]

            assert metric.hasDoubleGauge()
            def datapoints = metric.doubleGauge

            Map<String, String> expectedLabels = item[3]
            def expectedLabelCount = expectedLabels.size()

            assert datapoints.dataPointsCount == 1

            def datapoint = datapoints.getDataPoints(0)

            List<StringKeyValue> labels = datapoint.labelsList
            assert labels.size() == expectedLabelCount

            (0..<expectedLabelCount).each { j ->
                def key = labels[j].key
                assert expectedLabels.containsKey(key)
                def value = expectedLabels[key]
                if (!value.empty) {
                    def actual = labels[j].value
                    assert value.contains(actual)
                    value.remove(actual)
                    if (value.empty) {
                        expectedLabels.remove(key)
                    }
                }
            }

            assert expectedLabels == ['client-id': '']
        }

        cleanup:
        targetContainers.each { it.stop() }
        println jmxExtensionAppContainer.getLogs()
        jmxExtensionAppContainer.stop()
    }
}
