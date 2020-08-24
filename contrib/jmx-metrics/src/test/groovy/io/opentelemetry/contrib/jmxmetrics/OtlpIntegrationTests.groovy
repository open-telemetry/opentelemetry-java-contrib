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


import static io.opentelemetry.proto.metrics.v1.MetricDescriptor.Type.SUMMARY
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
import io.opentelemetry.proto.metrics.v1.MetricDescriptor
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import io.opentelemetry.proto.metrics.v1.SummaryDataPoint
import org.testcontainers.Testcontainers
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(60)
class OtlpIntegrationTests extends IntegrationTest  {

    @Shared
    def collector = new Collector()
    @Shared
    def collectorServer = ServerBuilder.forPort(55680).addService(collector).build()

    def setupSpec() {
        Testcontainers.exposeHostPorts(55680)
        configureContainers('otlp_config.properties', 0)
    }

    def setup() {
        collectorServer.start()
    }

    def cleanupSpec() {
        collectorServer.shutdown()
    }

    def 'end to end'() {
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
        il.name  == 'jmx-metrics'
        il.version == '0.0.1'

        when: 'we examine the instrumentation library metric metrics list'
        List<Metric> metrics = ilMetric.metricsList
        then: 'it is of the expected size'
        metrics.size() == 1

        when: 'we examine the metric descriptor'
        Metric metric = metrics.get(0)
        MetricDescriptor md = metric.metricDescriptor
        then: 'it is of the expected content'
        md.name == 'cassandra.storage.load'
        md.description == 'Size, in bytes, of the on disk data size this node manages'
        md.unit == 'By'
        md.type == SUMMARY

        when: 'we examine the datapoints'
        List<SummaryDataPoint> datapoints = metric.summaryDataPointsList
        then: 'they are of the expected size'
        datapoints.size() == 1

        when: 'we example the datapoint labels and sum'
        SummaryDataPoint datapoint = datapoints.get(0)
        List<StringKeyValue> labels = datapoint.labelsList
        def sum = datapoint.sum
        then: 'they are of the expected content'
        labels.size() == 1
        labels.get(0) == StringKeyValue.newBuilder().setKey("myKey").setValue("myVal").build()
        datapoint.count == 1
        datapoint.getPercentileValues(0).value == sum
        datapoint.getPercentileValues(1).value == sum
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
