/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import static org.awaitility.Awaitility.await

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
class KafkaTargetSystemIntegrationTests extends OtlpIntegrationTest {

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use Kafka as target system'
        targets = ["kafka"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('target-systems/kafka.properties',  otlpPort, 0, false)

        ArrayList<Metric> metrics
        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            List<ResourceMetrics> receivedMetrics = collector.receivedMetrics
            assert receivedMetrics.size() == 1

            ResourceMetrics receivedMetric = receivedMetrics.get(0)
            List<InstrumentationLibraryMetrics> ilMetrics =
                    receivedMetric.instrumentationLibraryMetricsList
            assert ilMetrics.size() == 1

            InstrumentationLibraryMetrics ilMetric = ilMetrics.get(0)
            metrics = ilMetric.metricsList as ArrayList
            metrics.sort{ a, b -> a.name <=> b.name}
            assert metrics.size() == 21
        }

        def expectedMetrics = [
            [
                'kafka.bytes.in',
                'bytes in per second from clients',
                'by',
                Gauge,
            ],
            [
                'kafka.bytes.out',
                'bytes out per second to clients',
                'by',
                Gauge,
            ],
            [
                'kafka.controller.active.count',
                'controller is active on broker',
                '1',
                Gauge,
            ],
            [
                'kafka.fetch.consumer.total.time.99p',
                'fetch consumer request time - 99th percentile',
                'ms',
                Gauge,
            ],
            [
                'kafka.fetch.consumer.total.time.count',
                'fetch consumer request count',
                '1',
                Sum,
            ],
            [
                'kafka.fetch.consumer.total.time.median',
                'fetch consumer request time - 50th percentile',
                'ms',
                Gauge,
            ],
            [
                'kafka.fetch.follower.total.time.99p',
                'fetch follower request time - 99th percentile',
                'ms',
                Gauge,
            ],
            [
                'kafka.fetch.follower.total.time.count',
                'fetch follower request count',
                '1',
                Sum,
            ],
            [
                'kafka.fetch.follower.total.time.median',
                'fetch follower request time - 50th percentile',
                'ms',
                Gauge,
            ],
            [
                'kafka.isr.expands',
                'in-sync replica expands per second',
                '1',
                Gauge,
            ],
            [
                'kafka.isr.shrinks',
                'in-sync replica shrinks per second',
                '1',
                Gauge,
            ],
            [
                'kafka.leader.election.rate',
                'leader election rate - non-zero indicates broker failures',
                '1',
                Gauge,
            ],
            [
                'kafka.max.lag',
                'max lag in messages between follower and leader replicas',
                '1',
                Gauge,
            ],
            [
                'kafka.messages.in',
                'number of messages in per second',
                '1',
                Gauge,
            ],
            [
                'kafka.partitions.offline.count',
                'number of partitions without an active leader',
                '1',
                Gauge,
            ],
            [
                'kafka.partitions.underreplicated.count',
                'number of under replicated partitions',
                '1',
                Gauge,
            ],
            [
                'kafka.produce.total.time.99p',
                'produce request time - 99th percentile',
                'ms',
                Gauge,
            ],
            [
                'kafka.produce.total.time.count',
                'produce request count',
                '1',
                Sum,
            ],
            [
                'kafka.produce.total.time.median',
                'produce request time - 50th percentile',
                'ms',
                Gauge,
            ],
            [
                'kafka.request.queue',
                'size of the request queue',
                '1',
                Gauge,
            ],
            [
                'kafka.unclean.election.rate',
                'unclean leader election rate - non-zero indicates broker failures',
                '1',
                Gauge,
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
