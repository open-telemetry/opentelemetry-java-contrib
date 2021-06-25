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


import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.SUMMARY
import static java.lang.management.ManagementFactory.getPlatformMBeanServer

import io.opentelemetry.api.metrics.common.Labels
import io.opentelemetry.api.metrics.GlobalMetricsProvider
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
            it.setProperty(JmxConfig.METRICS_EXPORTER_TYPE, 'inmemory')
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
        def provider = GlobalMetricsProvider.get().get(name.methodName, '')
        return provider.collectAll(0).sort { md1, md2 ->
            def p1 = md1.data.points[0]
            def p2 = md2.data.points[0]
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
            assert metric.type == metricType
            assert metric.data.points.size() == isSingle ? 1 : 4
            metric.data.points.eachWithIndex { point, i ->
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
        true | "single" | "Double" | "doubleCounter" | DOUBLE_SUM | 123.456
        false | "multiple" | "Double" | "doubleCounter" | DOUBLE_SUM | 123.456
        true | "single" | "Double" | "doubleUpDownCounter" | DOUBLE_SUM | 123.456
        false | "multiple" | "Double" | "doubleUpDownCounter" | DOUBLE_SUM | 123.456
        true | "single" | "Long" | "longCounter" | LONG_SUM | 234
        false | "multiple" | "Long" | "longCounter" | LONG_SUM | 234
        true | "single" | "Long" | "longUpDownCounter" | LONG_SUM | 234
        false | "multiple" | "Long" | "longUpDownCounter" | LONG_SUM | 234
        true | "single" | "Double" | "doubleValueRecorder" | SUMMARY | 123.456
        false | "multiple" | "Double" | "doubleValueRecorder" | SUMMARY | 123.456
        true | "single" | "Long" | "longValueRecorder" | SUMMARY | 234
        false | "multiple" | "Long" | "longValueRecorder" | SUMMARY | 234
        true | "single" | "Double" | "doubleSumObserver" | DOUBLE_SUM | 123.456
        false | "multiple" | "Double" | "doubleSumObserver" | DOUBLE_SUM | 123.456
        true | "single" | "Double" | "doubleUpDownSumObserver" | DOUBLE_SUM | 123.456
        false | "multiple" | "Double" | "doubleUpDownSumObserver" | DOUBLE_SUM | 123.456
        true | "single" | "Long" | "longSumObserver" | LONG_SUM | 234
        false | "multiple" | "Long" | "longSumObserver" | LONG_SUM | 234
        true | "single" | "Long" | "longUpDownSumObserver" | LONG_SUM | 234
        false | "multiple" | "Long" | "longUpDownSumObserver" | LONG_SUM | 234
        true | "single" | "Double" | "doubleValueObserver" | DOUBLE_GAUGE | 123.456
        false | "multiple" | "Double" | "doubleValueObserver" | DOUBLE_GAUGE | 123.456
        true | "single" | "Long" | "longValueObserver" | LONG_GAUGE | 234
        false | "multiple" | "Long" | "longValueObserver" | LONG_GAUGE | 234
    }

    @Unroll
    def "handles nulls returned from MBeanHelper"() {
        setup: "Create and register four Things and create ${quantity} MBeanHelper"
        def thingName = "${quantity}:type=${instrumentMethod}.${attribute}.Thing"
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
        metrics.size() == 0

        where:
        isSingle | quantity | attribute | instrumentMethod | metricType | value
        true | "single" | "Missing" | "longValueObserver" | LONG_GAUGE | null
        false | "multiple" | "Missing" | "longValueObserver" | LONG_GAUGE | null
    }

    @Unroll
    def "#instrumentMethod correctly classified"() {
        expect:
        def instrument = otel.&"${instrumentMethod}"
        assert InstrumentHelper.instrumentIsObserver(instrument) == isObserver
        assert InstrumentHelper.instrumentIsCounter(instrument) == isCounter

        where:
        instrumentMethod | isObserver | isCounter
        "doubleCounter" | false | true
        "longCounter" | false | true
        "doubleSumObserver" | true | false
        "longSumObserver" | true | false
        "doubleUpDownCounter" | false | true
        "longUpDownCounter" | false | true
        "doubleUpDownSumObserver" | true | false
        "longUpDownSumObserver" | true | false
        "doubleValueObserver" | true | false
        "longValueObserver" | true | false
        "doubleValueRecorder" | false | false
        "longValueRecorder" | false | false
    }
}
