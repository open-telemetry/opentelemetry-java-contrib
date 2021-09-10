/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import static org.awaitility.Awaitility.await

import io.opentelemetry.proto.common.v1.InstrumentationLibrary
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.metrics.v1.Gauge
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import io.opentelemetry.proto.metrics.v1.Sum
import java.util.concurrent.TimeUnit
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(90)
class CassandraIntegrationTests extends OtlpIntegrationTest  {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use Cassandra as target system'
        targets = ["cassandra"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/cassandra.properties',  otlpPort, 0, false)

        ArrayList<Metric> metrics
        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            List<ResourceMetrics> receivedMetrics = collector.receivedMetrics
            assert receivedMetrics.size() == 1

            ResourceMetrics receivedMetric = receivedMetrics.get(0)
            List<InstrumentationLibraryMetrics> ilMetrics =
                    receivedMetric.instrumentationLibraryMetricsList
            assert ilMetrics.size() == 1

            InstrumentationLibraryMetrics ilMetric = ilMetrics.get(0)
            InstrumentationLibrary il = ilMetric.instrumentationLibrary
            assert il.name  == 'io.opentelemetry.contrib.jmxmetrics'
            assert il.version == expectedMeterVersion()

            metrics = ilMetric.metricsList as ArrayList
            metrics.sort{ a, b -> a.name <=> b.name}
            assert metrics.size() == 23
        }

        def expectedMetrics = [
            [
                'cassandra.client.request.range_slice.latency.50p',
                'Token range read request latency - 50th percentile',
                'µs',
                Gauge
            ],
            [
                'cassandra.client.request.range_slice.latency.99p',
                'Token range read request latency - 99th percentile',
                'µs',
                Gauge
            ],
            [
                'cassandra.client.request.range_slice.latency.count',
                'Total token range read request latency',
                'µs',
                Sum
            ],
            [
                'cassandra.client.request.range_slice.latency.max',
                'Maximum token range read request latency',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.range_slice.timeout.count',
                'Number of token range read request timeouts encountered',
                '1',
                Sum,
            ],
            [
                'cassandra.client.request.range_slice.unavailable.count',
                'Number of token range read request unavailable exceptions encountered',
                '1',
                Sum,
            ],
            [
                'cassandra.client.request.read.latency.50p',
                'Standard read request latency - 50th percentile',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.read.latency.99p',
                'Standard read request latency - 99th percentile',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.read.latency.count',
                'Total standard read request latency',
                'µs',
                Sum,
            ],
            [
                'cassandra.client.request.read.latency.max',
                'Maximum standard read request latency',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.read.timeout.count',
                'Number of standard read request timeouts encountered',
                '1',
                Sum,
            ],
            [
                'cassandra.client.request.read.unavailable.count',
                'Number of standard read request unavailable exceptions encountered',
                '1',
                Sum,
            ],
            [
                'cassandra.client.request.write.latency.50p',
                'Regular write request latency - 50th percentile',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.write.latency.99p',
                'Regular write request latency - 99th percentile',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.write.latency.count',
                'Total regular write request latency',
                'µs',
                Sum,
            ],
            [
                'cassandra.client.request.write.latency.max',
                'Maximum regular write request latency',
                'µs',
                Gauge,
            ],
            [
                'cassandra.client.request.write.timeout.count',
                'Number of regular write request timeouts encountered',
                '1',
                Sum,
            ],
            [
                'cassandra.client.request.write.unavailable.count',
                'Number of regular write request unavailable exceptions encountered',
                '1',
                Sum,
            ],
            [
                'cassandra.compaction.tasks.completed',
                'Number of completed compactions since server [re]start',
                '1',
                Sum,
            ],
            [
                'cassandra.compaction.tasks.pending',
                'Estimated number of compactions remaining to perform',
                '1',
                Gauge,
            ],
            [
                'cassandra.storage.load.count',
                'Size of the on disk data size this node manages',
                'by',
                Sum,
            ],
            [
                'cassandra.storage.total_hints.count',
                'Number of hint messages written to this node since [re]start',
                '1',
                Sum,
            ],
            [
                'cassandra.storage.total_hints.in_progress.count',
                'Number of hints attempting to be sent currently',
                '1',
                Sum,
            ],
        ].eachWithIndex{ item, index ->
            Metric metric = metrics.get(index)
            assert metric.name == item[0]
            assert metric.description == item[1]
            assert metric.unit == item[2]
            def datapoint
            switch(item[3]) {
                case Gauge:
                    assert metric.hasGauge()
                    Gauge datapoints = metric.gauge
                    assert datapoints.dataPointsCount == 1
                    datapoint = datapoints.getDataPoints(0)
                    break
                case Sum:
                    assert metric.hasSum()
                    Sum datapoints = metric.sum
                    assert datapoints.dataPointsCount == 1
                    datapoint = datapoints.getDataPoints(0)
                    break
                default:
                    assert false, "Invalid expected data type: ${item[3]}"
            }
            List<KeyValue> labels = datapoint.attributesList
            assert labels.size() == 0
        }

        cleanup:
        targetContainers.each { it.stop() }
        jmxExtensionAppContainer.stop()
    }
}
