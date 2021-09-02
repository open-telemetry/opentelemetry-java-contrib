/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import static org.awaitility.Awaitility.await

import io.opentelemetry.proto.common.v1.InstrumentationLibrary
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.IntGauge
import io.opentelemetry.proto.metrics.v1.IntSum
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import java.util.concurrent.TimeUnit
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(90)
class JVMTargetSystemIntegrationTests extends OtlpIntegrationTest {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use JVM as target system'
        targets = ["cassandra"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/jvm.properties',  otlpPort, 0, false, "script.groovy")

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
            assert il.version == '1.0.0-alpha'

            when: 'we examine the instrumentation library metric metrics list'
            metrics = ilMetric.metricsList as ArrayList
            metrics.sort{ a, b -> a.name <=> b.name}
            assert metrics.size() == 16
        }

        def expectedMetrics = [
            [
                'jvm.classes.loaded',
                'number of loaded classes',
                '1',
                [],
                IntGauge
            ],
            [
                'jvm.gc.collections.count',
                'total number of collections that have occurred',
                '1',
                [
                    "ConcurrentMarkSweep",
                    "ParNew"
                ],
                IntSum
            ],
            [
                'jvm.gc.collections.elapsed',
                'the approximate accumulated collection elapsed time in milliseconds',
                'ms',
                [
                    "ConcurrentMarkSweep",
                    "ParNew"
                ],
                IntSum
            ],
            [
                'jvm.memory.heap.committed',
                'current heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.heap.init',
                'current heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.heap.max',
                'current heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.heap.used',
                'current heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.nonheap.committed',
                'current non-heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.nonheap.init',
                'current non-heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.nonheap.max',
                'current non-heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.nonheap.used',
                'current non-heap usage',
                'by',
                [],
                IntGauge
            ],
            [
                'jvm.memory.pool.committed',
                'current memory pool usage',
                'by',
                [
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space"
                ],
                IntGauge
            ],
            [
                'jvm.memory.pool.init',
                'current memory pool usage',
                'by',
                [
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space"
                ],
                IntGauge
            ],
            [
                'jvm.memory.pool.max',
                'current memory pool usage',
                'by',
                [
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space"
                ],
                IntGauge
            ],
            [
                'jvm.memory.pool.used',
                'current memory pool usage',
                'by',
                [
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space"
                ],
                IntGauge
            ],
            [
                'jvm.threads.count',
                'number of threads',
                '1',
                [],
                IntGauge
            ],
        ].eachWithIndex{ item, index ->
            def expectedType = item[4]

            Metric metric = metrics.get(index)
            assert metric.name == item[0]
            assert metric.description == item[1]
            assert metric.unit == item[2]

            def datapoints
            if (expectedType == IntGauge) {
                assert metric.hasGauge()
                datapoints = metric.gauge
            } else {
                assert metric.hasSum()
                datapoints = metric.sum
            }
            def expectedLabelCount = item[3].size()
            def expectedLabels = item[3] as Set

            def expectedDatapointCount = expectedLabelCount == 0 ? 1 : expectedLabelCount
            assert datapoints.dataPointsCount == expectedDatapointCount

            (0..<expectedDatapointCount).each { i ->
                def datapoint = datapoints.getDataPoints(i)
                List<KeyValue> labels = datapoint.attributesList
                if (expectedLabelCount != 0) {
                    assert labels.size() == 1
                    assert labels[0].key == 'name'
                    def value = labels[0].value.stringValue
                    assert expectedLabels.remove(value)
                } else {
                    assert labels.size() == 0
                }
            }

            assert expectedLabels.size() == 0
        }

        cleanup:
        targetContainers.each { it.stop() }
        jmxExtensionAppContainer.stop()
    }
}
