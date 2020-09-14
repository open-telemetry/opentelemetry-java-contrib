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

import static org.junit.Assert.assertTrue

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc
import io.opentelemetry.proto.common.v1.InstrumentationLibrary
import io.opentelemetry.proto.common.v1.StringKeyValue
import io.opentelemetry.proto.metrics.v1.InstrumentationLibraryMetrics
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.DoubleHistogram
import io.opentelemetry.proto.metrics.v1.DoubleHistogramDataPoint
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import java.util.concurrent.TimeUnit
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Timeout
import spock.lang.Unroll

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(60)
class OtlpIntegrationTests extends IntegrationTest  {

    @Shared
    def collector
    @Shared
    def collectorServer
    @Shared
    def otlpPort

    def setup() {
        // set up a collector per test to avoid noisy neighbor
        otlpPort = availablePort()
        collector = new Collector()
        collectorServer = ServerBuilder.forPort(otlpPort).addService(collector).build()
        collectorServer.start()
    }

    def cleanup() {
        collectorServer.shutdownNow()
        collectorServer.awaitTermination(5, TimeUnit.SECONDS)
    }

    def availablePort() {
        def sock = new ServerSocket(0);
        def port = sock.getLocalPort()
        sock.close()
        return port
    }

    @Unroll
    def 'end to end with stdin config: #useStdin'() {
        setup: 'we configure JMX metrics gatherer and target server'
        Testcontainers.exposeHostPorts(otlpPort)
        configureContainers('otlp_config.properties',  otlpPort, 0, useStdin)

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
        List<Metric> metrics = ilMetric.metricsList
        then: 'it is of the expected size'
        metrics.size() == 1

        when: 'we examine the metric metadata'
        Metric metric = metrics.get(0)
        then: 'it is of the expected content'
        metric.name == 'cassandra.storage.load'
        metric.description == 'Size, in bytes, of the on disk data size this node manages'
        metric.unit == 'By'
        metric.hasDoubleHistogram()

        when: 'we examine the datapoints'
        DoubleHistogram datapoints = metric.doubleHistogram
        then: 'they are of the expected size'
        datapoints.dataPointsCount == 1

        when: 'we example the datapoint labels and sum'
        DoubleHistogramDataPoint datapoint = datapoints.getDataPoints(0)
        List<StringKeyValue> labels = datapoint.labelsList
        def sum = datapoint.sum
        then: 'they are of the expected content'
        labels.size() == 1
        labels.get(0) == StringKeyValue.newBuilder().setKey("myKey").setValue("myVal").build()

        datapoint.count == 1
        datapoint.getBucketCounts(0).value == sum
        datapoint.getBucketCounts(1).value == sum

        cleanup:
        cassandraContainer.stop()
        jmxExtensionAppContainer.stop()

        where:
        useStdin | _
        false | _
        true | _
    }

    static final class Collector extends MetricsServiceGrpc.MetricsServiceImplBase {
        private final List<ResourceMetrics> receivedMetrics = new ArrayList<>()
        private final Object monitor = new Object()

        @Override
        void export(
                ExportMetricsServiceRequest request,
                StreamObserver<ExportMetricsServiceResponse> responseObserver) {
            synchronized (receivedMetrics) {
                receivedMetrics.addAll(request.resourceMetricsList)
            }
            synchronized (monitor) {
                monitor.notify()
            }
            responseObserver.onNext(ExportMetricsServiceResponse.newBuilder().build())
            responseObserver.onCompleted()
        }

        List<ResourceMetrics> getReceivedMetrics() {
            List<ResourceMetrics> received
            try {
                synchronized (monitor) {
                    monitor.wait(15000)
                }
            } catch (final InterruptedException e) {
                assertTrue(e.message, false)
            }

            synchronized (receivedMetrics) {
                received = new ArrayList<>(receivedMetrics)
                receivedMetrics.clear()
            }
            return received
        }
    }
}
