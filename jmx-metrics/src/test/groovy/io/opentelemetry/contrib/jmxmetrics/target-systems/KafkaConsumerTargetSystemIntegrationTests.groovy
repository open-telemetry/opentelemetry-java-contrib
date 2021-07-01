/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(90)
class KafkaConsumerTargetSystemIntegrationTests extends OtlpIntegrationTest {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use Kafka as target system'
        targets = ["kafka-consumer"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/kafka-consumer.properties',  otlpPort, 0, false)

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
        metrics.size() == 8

        def expectedTopics = [
            'test-topic-1',
            'test-topic-2',
            'test-topic-3'
        ]

        [
            [
                'kafka.consumer.bytes-consumed-rate',
                'The average number of bytes consumed per second',
                'by',
                ['client-id' : '', 'topic' : expectedTopics.clone() ],
            ],
            [
                'kafka.consumer.fetch-rate',
                'The number of fetch requests for all topics per second',
                '1',
                ['client-id' : '']
            ],
            [
                'kafka.consumer.fetch-size-avg',
                'The average number of bytes fetched per request',
                'by',
                ['client-id' : '', 'topic' : expectedTopics.clone() ],
            ],
            [
                'kafka.consumer.records-consumed-rate',
                'The average number of records consumed per second',
                '1',
                ['client-id' : '', 'topic' : expectedTopics.clone() ],
            ],
            [
                'kafka.consumer.records-lag-max',
                'Number of messages the consumer lags behind the producer',
                '1',
                ['client-id' : '']
            ],
            [
                'kafka.consumer.total.bytes-consumed-rate',
                'The average number of bytes consumed for all topics per second',
                'by',
                ['client-id' : '']
            ],
            [
                'kafka.consumer.total.fetch-size-avg',
                'The average number of bytes fetched per request for all topics',
                'by',
                ['client-id' : '']
            ],
            [
                'kafka.consumer.total.records-consumed-rate',
                'The average number of records consumed for all topics per second',
                '1',
                ['client-id' : '']
            ],
        ].eachWithIndex{ item, index ->
            Metric metric = metrics.get(index)
            assert metric.name == item[0]
            assert metric.description == item[1]
            assert metric.unit == item[2]

            assert metric.hasGauge()
            def datapoints = metric.gauge

            Map<String, String> expectedLabels = item[3]
            def expectedLabelCount = expectedLabels.size()

            assert datapoints.dataPointsCount == expectedLabelCount == 1 ? 1 : 3

            (0..<datapoints.dataPointsCount).each { i ->
                def datapoint = datapoints.getDataPoints(i)

                List<KeyValue> labels = datapoint.attributesList
                assert labels.size() == expectedLabelCount

                (0..<expectedLabelCount).each { j ->
                    def key = labels[j].key
                    assert expectedLabels.containsKey(key)
                    def value = expectedLabels[key]
                    if (!value.empty) {
                        def actual = labels[j].value.stringValue
                        assert value.contains(actual)
                        value.remove(actual)
                        if (value.empty) {
                            expectedLabels.remove(key)
                        }
                    }
                }
            }

            assert expectedLabels == ['client-id': '']
        }

        cleanup:
        targetContainers.each { it.stop() }
        jmxExtensionAppContainer.stop()
    }
}
