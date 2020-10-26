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

import io.opentelemetry.proto.metrics.v1.DoubleSum
import io.opentelemetry.proto.metrics.v1.IntSum

import io.opentelemetry.proto.common.v1.InstrumentationLibrary
import io.opentelemetry.proto.common.v1.StringKeyValue
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(60)
class CassandraIntegrationTests extends OtlpIntegrationTest  {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use Cassandra as target system'
        targets = ["cassandra"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/cassandra.properties',  otlpPort, 0, false)

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
        il.version == '0.0.1'

        when: 'we examine the instrumentation library metric metrics list'
        ArrayList<Metric> metrics = ilMetric.metricsList as ArrayList
        metrics.sort{ a, b -> a.name <=> b.name}
        then: 'they are of the expected size and content'
        metrics.size() == 23

        def expectedMetrics = [
            [
                'cassandra.client.request.range_slice.latency.50p',
                'Token range read request latency - 50th percentile',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.range_slice.latency.99p',
                'Token range read request latency - 99th percentile',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.range_slice.latency.count',
                'Total token range read request latency',
                'µs',
                'int',
            ],
            [
                'cassandra.client.request.range_slice.latency.max',
                'Maximum token range read request latency',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.range_slice.timeout.count',
                'Number of token range read request timeouts encountered',
                '1',
                'int',
            ],
            [
                'cassandra.client.request.range_slice.unavailable.count',
                'Number of token range read request unavailable exceptions encountered',
                '1',
                'int',
            ],
            [
                'cassandra.client.request.read.latency.50p',
                'Standard read request latency - 50th percentile',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.read.latency.99p',
                'Standard read request latency - 99th percentile',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.read.latency.count',
                'Total standard read request latency',
                'µs',
                'int',
            ],
            [
                'cassandra.client.request.read.latency.max',
                'Maximum standard read request latency',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.read.timeout.count',
                'Number of standard read request timeouts encountered',
                '1',
                'int',
            ],
            [
                'cassandra.client.request.read.unavailable.count',
                'Number of standard read request unavailable exceptions encountered',
                '1',
                'int',
            ],
            [
                'cassandra.client.request.write.latency.50p',
                'Regular write request latency - 50th percentile',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.write.latency.99p',
                'Regular write request latency - 99th percentile',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.write.latency.count',
                'Total regular write request latency',
                'µs',
                'int',
            ],
            [
                'cassandra.client.request.write.latency.max',
                'Maximum regular write request latency',
                'µs',
                'double',
            ],
            [
                'cassandra.client.request.write.timeout.count',
                'Number of regular write request timeouts encountered',
                '1',
                'int',
            ],
            [
                'cassandra.client.request.write.unavailable.count',
                'Number of regular write request unavailable exceptions encountered',
                '1',
                'int',
            ],
            [
                'cassandra.compaction.tasks.completed',
                'Number of completed compactions since server [re]start',
                '1',
                'int',
            ],
            [
                'cassandra.compaction.tasks.pending',
                'Estimated number of compactions remaining to perform',
                '1',
                'int',
            ],
            [
                'cassandra.storage.load.count',
                'Size of the on disk data size this node manages',
                'by',
                'int',
            ],
            [
                'cassandra.storage.total_hints.count',
                'Number of hint messages written to this node since [re]start',
                '1',
                'int',
            ],
            [
                'cassandra.storage.total_hints.in_progress.count',
                'Number of hints attempting to be sent currently',
                '1',
                'int',
            ],
        ].eachWithIndex{ item, index ->
            Metric metric = metrics.get(index)
            assert metric.name == item[0]
            assert metric.description == item[1]
            assert metric.unit == item[2]
            def datapoint
            switch(item[3]) {
                case 'double':
                    assert metric.hasDoubleSum()
                    DoubleSum datapoints = metric.doubleSum
                    assert datapoints.dataPointsCount == 1
                    datapoint = datapoints.getDataPoints(0)
                    break
                case 'int':
                    assert metric.hasIntSum()
                    IntSum datapoints = metric.intSum
                    assert datapoints.dataPointsCount == 1
                    datapoint = datapoints.getDataPoints(0)
                    break
                default:
                    assert false, "Invalid expected data type: ${item[3]}"
            }
            List<StringKeyValue> labels = datapoint.labelsList
            assert labels.size() == 0
        }

        cleanup:
        targetContainers.each { it.stop() }
        jmxExtensionAppContainer.stop()
    }
}
