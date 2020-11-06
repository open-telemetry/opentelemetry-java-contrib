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
import io.opentelemetry.proto.metrics.v1.DoubleSum
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.IntSum
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(60)
class KafkaTargetSystemIntegrationTests extends OtlpIntegrationTest {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use Kafka as target system'
        targets = ["kafka"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/kafka.properties',  otlpPort, 0, false)

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
        metrics.size() == 21

        def expectedMetrics = [
            [
                'kafka.bytes.in',
                'bytes in per second from clients',
                'by',
                'int',
            ],
            [
                'kafka.bytes.out',
                'bytes out per second to clients',
                'by',
                'int',
            ],
            [
                'kafka.controller.active.count',
                'controller is active on broker',
                '1',
                'int',
            ],
            [
                'kafka.fetch.consumer.total.time.99p',
                'fetch consumer request time - 99th percentile',
                'ms',
                'double',
            ],
            [
                'kafka.fetch.consumer.total.time.count',
                'fetch consumer request count',
                '1',
                'int',
            ],
            [
                'kafka.fetch.consumer.total.time.median',
                'fetch consumer request time - 50th percentile',
                'ms',
                'double',
            ],
            [
                'kafka.fetch.follower.total.time.99p',
                'fetch follower request time - 99th percentile',
                'ms',
                'double',
            ],
            [
                'kafka.fetch.follower.total.time.count',
                'fetch follower request count',
                '1',
                'int',
            ],
            [
                'kafka.fetch.follower.total.time.median',
                'fetch follower request time - 50th percentile',
                'ms',
                'double',
            ],
            [
                'kafka.isr.expands',
                'in-sync replica expands per second',
                '1',
                'int',
            ],
            [
                'kafka.isr.shrinks',
                'in-sync replica shrinks per second',
                '1',
                'int',
            ],
            [
                'kafka.leader.election.rate',
                'leader election rate - non-zero indicates broker failures',
                '1',
                'int',
            ],
            [
                'kafka.max.lag',
                'max lag in messages between follower and leader replicas',
                '1',
                'int',
            ],
            [
                'kafka.messages.in',
                'number of messages in per second',
                '1',
                'int',
            ],
            [
                'kafka.partitions.offline.count',
                'number of partitions without an active leader',
                '1',
                'int',
            ],
            [
                'kafka.partitions.underreplicated.count',
                'number of under replicated partitions',
                '1',
                'int',
            ],
            [
                'kafka.produce.total.time.99p',
                'produce request time - 99th percentile',
                'ms',
                'double',
            ],
            [
                'kafka.produce.total.time.count',
                'produce request count',
                '1',
                'int',
            ],
            [
                'kafka.produce.total.time.median',
                'produce request time - 50th percentile',
                'ms',
                'double',
            ],
            [
                'kafka.request.queue',
                'size of the request queue',
                '1',
                'int',
            ],
            [
                'kafka.unclean.election.rate',
                'unclean leader election rate - non-zero indicates broker failures',
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
