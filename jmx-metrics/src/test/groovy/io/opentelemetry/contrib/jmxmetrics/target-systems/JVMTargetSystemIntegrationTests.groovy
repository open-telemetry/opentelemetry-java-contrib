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

import io.opentelemetry.proto.metrics.v1.IntGauge
import io.opentelemetry.proto.metrics.v1.IntSum

import io.opentelemetry.proto.common.v1.InstrumentationLibrary
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
class JVMTargetSystemIntegrationTests extends OtlpIntegrationTest {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use JVM as target system'
        targets = ["cassandra"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/jvm.properties',  otlpPort, 0, false)

        expect:
        when: 'we receive metrics from the JMX metrics gatherer'
        List<ResourceMetrics> receivedMetrics = collector.receivedMetrics
        then: 'they are of the expected size'
        receivedMetrics.size() == 1

        when: "we examine the received metric's instrumentation library metrics lists"
        ResourceMetrics receivedMetric = receivedMetrics.get(0)
        List<InstrumentationLibraryMetrics> ilMetrics =
                receivedMetric.instrumentationLibraryMetricsList
        then: 'they of the expected size'
        ilMetrics.size() == 1

        when: 'we examine the instrumentation library'
        InstrumentationLibraryMetrics ilMetric = ilMetrics.get(0)
        InstrumentationLibrary il = ilMetric.instrumentationLibrary
        then: 'it is of the expected content'
        il.name  == 'io.opentelemetry.contrib.jmxmetrics'
        il.version == '1.0.0-alpha'

        when: 'we examine the instrumentation library metric metrics list'
        ArrayList<Metric> metrics = ilMetric.metricsList as ArrayList
        metrics.sort{ a, b -> a.name <=> b.name}
        then: 'they are of the expected size and content'
        metrics.size() == 16

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
                assert metric.hasIntGauge()
                datapoints = metric.intGauge
            } else {
                assert metric.hasIntSum()
                datapoints = metric.intSum
            }
            def expectedLabelCount = item[3].size()
            def expectedLabels = item[3] as Set

            def expectedDatapointCount = expectedLabelCount == 0 ? 1 : expectedLabelCount
            assert datapoints.dataPointsCount == expectedDatapointCount

            (0..<expectedDatapointCount).each { i ->
                def datapoint = datapoints.getDataPoints(i)
                List<StringKeyValue> labels = datapoint.labelsList
                if (expectedLabelCount != 0) {
                    assert labels.size() == 1
                    assert labels[0].key == 'name'
                    def value = labels[0].value
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
