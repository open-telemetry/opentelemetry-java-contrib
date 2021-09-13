/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import io.opentelemetry.proto.common.v1.AnyValue
import io.opentelemetry.proto.common.v1.InstrumentationLibrary
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.metrics.v1.Histogram
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Timeout
import spock.lang.Unroll

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(90)
class OtlpIntegrationTests extends OtlpIntegrationTest {

    @Unroll
    def 'end to end with stdin config: #useStdin'() {
        setup: 'we configure JMX metrics gatherer and target server'
        targets = ["cassandra"]
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('otlp_config.properties',  otlpPort, 0, useStdin, "script.groovy")

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
        il.version == expectedMeterVersion()

        when: 'we examine the instrumentation library metric metrics list'
        List<Metric> metrics = ilMetric.metricsList
        then: 'it is of the expected size'
        metrics.size() == 1

        when: 'we examine the metric metadata'
        Metric metric = metrics.get(0)
        then: 'it is of the expected content'
        metric.name == 'cassandra.storage.load'
        metric.description == 'Size, in bytes, of the on disk data size this node manages'
        metric.unit == 'By'
        metric.hasHistogram()

        when: 'we examine the datapoints'
        Histogram datapoints = metric.histogram
        then: 'they are of the expected size'
        datapoints.dataPointsCount == 1

        when: 'we example the datapoint labels and sum'
        HistogramDataPoint datapoint = datapoints.getDataPoints(0)
        List<KeyValue> attributes = datapoint.attributesList
        def sum = datapoint.sum
        then: 'they are of the expected content'
        attributes.size() == 1
        attributes.get(0) == KeyValue.newBuilder().setKey("myKey").setValue(AnyValue.newBuilder().setStringValue("myVal")).build()

        datapoint.count == 1
        datapoint.sum == sum

        cleanup:
        targetContainers.each { it.stop() }
        jmxExtensionAppContainer.stop()

        where:
        useStdin | _
        false | _
        true | _
    }
}
