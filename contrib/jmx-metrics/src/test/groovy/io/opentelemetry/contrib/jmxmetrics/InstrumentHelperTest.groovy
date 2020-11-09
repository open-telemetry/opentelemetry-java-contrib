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

import static io.opentelemetry.sdk.metrics.data.MetricData.Type.GAUGE_DOUBLE
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.GAUGE_LONG
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.MONOTONIC_DOUBLE
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.MONOTONIC_LONG
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.NON_MONOTONIC_DOUBLE
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.NON_MONOTONIC_LONG
import static io.opentelemetry.sdk.metrics.data.MetricData.Type.SUMMARY
import static java.lang.management.ManagementFactory.getPlatformMBeanServer

import io.opentelemetry.api.common.Labels
import io.opentelemetry.sdk.OpenTelemetrySdk
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.remote.JMXConnectorServer
import javax.management.remote.JMXConnectorServerFactory
import javax.management.remote.JMXServiceURL
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class InstrumentHelperTest extends Specification {

    @Rule public final TestRule name = new TestName()

    @Shared
    MBeanServer mBeanServer

    @Shared
    JMXConnectorServer jmxServer

    @Shared
    JmxClient jmxClient

    @Shared
    OtelHelper otel

    def setup() {
        mBeanServer = getPlatformMBeanServer()

        def serviceUrl = new JMXServiceURL('rmi', 'localhost', 0)
        jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, [:], mBeanServer)
        jmxServer.start()
        def completeAddress = jmxServer.getAddress()

        def jmxConfig = new JmxConfig(new Properties().tap {
            it.setProperty(JmxConfig.EXPORTER_TYPE, 'inmemory')
            it.setProperty(JmxConfig.SERVICE_URL, "${completeAddress}")
        })

        jmxClient = new JmxClient(jmxConfig)

        // Set up a MeterSdk per test to be able to collect its metrics alone
        def gme = new GroovyMetricEnvironment(jmxConfig, name.methodName, '')
        otel = new OtelHelper(jmxClient, gme)
    }

    def cleanup() {
        jmxServer.stop()
    }

    interface ThingMBean {
        double getDouble()
        long getLong()
    }

    static class Thing implements ThingMBean {
        @Override
        double getDouble() {
            return 123.456
        }

        @Override
        long getLong() {
            return 234
        }
    }

    def exportMetrics() {
        def provider = OpenTelemetrySdk.globalMeterProvider.get(name.methodName, '')
        return provider.collectAll().sort { md1, md2 ->
            def p1 = md1.points[0]
            def p2 = md2.points[0]
            def s1 = p1.startEpochNanos
            def s2 = p2.startEpochNanos
            if (s1 == s2) {
                if (md1.type == SUMMARY) {
                    return p1.percentileValues[0].value <=> p2.percentileValues[0].value
                }
                return p1.value <=> p2.value
            }
            s1 <=> s2
        }
    }

    @Unroll
    def "#instrumentMethod via #quantity MBeanHelper"() {
        setup: "Create and register four Things and create ${quantity} MBeanHelper"
        def thingName = "${quantity}:type=${instrumentMethod}.Thing"
        def things = (0..3).collect { new Thing() }
        things.eachWithIndex { thing, i ->
            def name = "${thingName},thing=${i}"
            mBeanServer.registerMBean(thing, new ObjectName(name))
        }
        def mbeanHelper = new MBeanHelper(jmxClient, "${thingName},*", isSingle)
        mbeanHelper.fetch()

        expect:
        when:
        def instrumentName = "${quantity}.${instrumentMethod}.counter"
        def description = "${quantity} double counter description"
        def instrument = otel.&"${instrumentMethod}"
        def instrumentHelper = new InstrumentHelper(
                mbeanHelper, instrumentName, description, "1",
                ["labelOne" : { "labelOneValue"}, "labelTwo": { mbean -> mbean.name().getKeyProperty("thing") }],
                attribute, instrument)
        instrumentHelper.update()

        then:
        def metrics = exportMetrics()
        metrics.size() == 1

        metrics.each { metric ->
            assert metric.name == instrumentName
            assert metric.description == description
            assert metric.unit == "1"
            assert metric.type ==  metricType
            assert metric.points.size() == isSingle ? 1 : 4
            metric.points.eachWithIndex { point, i ->
                assert point.labels == Labels.of("labelOne", "labelOneValue", "labelTwo", "${i}")

                if (metricType == SUMMARY) {
                    assert point.count == 1
                    assert point.sum == value
                    assert point.percentileValues[0].percentile == 0
                    assert point.percentileValues[0].value == value
                    assert point.percentileValues[1].percentile == 100
                    assert point.percentileValues[1].value == value
                } else {
                    assert point.value == value
                }
            }
        }

        where:
        isSingle | quantity | attribute | instrumentMethod | metricType | value
        true | "single" | "Double" | "doubleCounter" | MONOTONIC_DOUBLE | 123.456
        false | "multiple" | "Double" | "doubleCounter" | MONOTONIC_DOUBLE | 123.456
        true | "single" | "Double" | "doubleUpDownCounter" | NON_MONOTONIC_DOUBLE | 123.456
        false | "multiple" | "Double" | "doubleUpDownCounter" | NON_MONOTONIC_DOUBLE | 123.456
        true | "single" | "Long" | "longCounter" | MONOTONIC_LONG | 234
        false | "multiple" | "Long" | "longCounter" | MONOTONIC_LONG | 234
        true | "single" | "Long" | "longUpDownCounter" | NON_MONOTONIC_LONG | 234
        false | "multiple" | "Long" | "longUpDownCounter" | NON_MONOTONIC_LONG | 234
        true | "single" | "Double" | "doubleValueRecorder" | SUMMARY | 123.456
        false | "multiple" | "Double" | "doubleValueRecorder" | SUMMARY | 123.456
        true | "single" | "Long" | "longValueRecorder" | SUMMARY | 234
        false | "multiple" | "Long" | "longValueRecorder" | SUMMARY | 234
        true | "single" | "Double" | "doubleSumObserver" | MONOTONIC_DOUBLE | 123.456
        false | "multiple" | "Double" | "doubleSumObserver" | MONOTONIC_DOUBLE | 123.456
        true | "single" | "Double" | "doubleUpDownSumObserver" | NON_MONOTONIC_DOUBLE | 123.456
        false | "multiple" | "Double" | "doubleUpDownSumObserver" | NON_MONOTONIC_DOUBLE | 123.456
        true | "single" | "Long" | "longSumObserver" | MONOTONIC_LONG | 234
        false | "multiple" | "Long" | "longSumObserver" | MONOTONIC_LONG | 234
        true | "single" | "Long" | "longUpDownSumObserver" | NON_MONOTONIC_LONG | 234
        false | "multiple" | "Long" | "longUpDownSumObserver" | NON_MONOTONIC_LONG | 234
        true | "single" | "Double" | "doubleValueObserver" | GAUGE_DOUBLE | 123.456
        false | "multiple" | "Double" | "doubleValueObserver" | GAUGE_DOUBLE | 123.456
        true | "single" | "Long" | "longValueObserver" | GAUGE_LONG | 234
        false | "multiple" | "Long" | "longValueObserver" | GAUGE_LONG | 234
    }
}
