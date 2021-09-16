/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import static io.opentelemetry.api.common.AttributeKey.stringKey
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.HISTOGRAM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM
import static java.lang.management.ManagementFactory.getPlatformMBeanServer

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.GlobalMeterProvider
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

    @Rule
    public final TestRule name = new TestName()

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
        def provider = GlobalMeterProvider.get().get(name.methodName, '', null)
        return provider.collectAll(0).sort { md1, md2 ->
            def p1 = md1.data.points[0]
            def p2 = md2.data.points[0]
            def s1 = p1.startEpochNanos
            def s2 = p2.startEpochNanos
            if (s1 == s2) {
                if (md1.type == HISTOGRAM) {
                    return p1.counts[0] <=> p2.counts[0]
                }
                return p1.value <=> p2.value
            }
            s1 <=> s2
        }
    }

    @Unroll
    def "#instrumentMethod via #quantity MBeanHelper"() {
        setup:
        "Create and register four Things and create ${quantity} MBeanHelper"
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
                ["labelOne": { "labelOneValue" }, "labelTwo": { mbean -> mbean.name().getKeyProperty("thing") }],
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
            metric.data.points.sort { a, b -> String.compare(a.attributes.get(stringKey("labelTwo")), b.attributes.get(stringKey("labelTwo"))) }
            metric.data.points.eachWithIndex { point, i ->
                assert point.attributes == Attributes.of(stringKey("labelOne"), "labelOneValue", stringKey("labelTwo"), "${i}".toString())

                if (metricType == HISTOGRAM) {
                    assert point.count == 1
                    assert point.sum == value
                    assert point.counts[6].value == 1
                } else {
                    assert point.value == value
                }
            }
        }

        where:
        isSingle | quantity   | attribute | instrumentMethod              | metricType   | value
        true     | "single"   | "Double"  | "doubleCounter"               | DOUBLE_SUM   | 123.456
        false    | "multiple" | "Double"  | "doubleCounter"               | DOUBLE_SUM   | 123.456
        true     | "single"   | "Double"  | "doubleUpDownCounter"         | DOUBLE_SUM   | 123.456
        false    | "multiple" | "Double"  | "doubleUpDownCounter"         | DOUBLE_SUM   | 123.456
        true     | "single"   | "Long"    | "longCounter"                 | LONG_SUM     | 234
        false    | "multiple" | "Long"    | "longCounter"                 | LONG_SUM     | 234
        true     | "single"   | "Long"    | "longUpDownCounter"           | LONG_SUM     | 234
        false    | "multiple" | "Long"    | "longUpDownCounter"           | LONG_SUM     | 234
        true     | "single"   | "Double"  | "doubleHistogram"             | HISTOGRAM      | 123.456
        false    | "multiple" | "Double"  | "doubleHistogram"             | HISTOGRAM      | 123.456
        true     | "single"   | "Long"    | "longHistogram"               | HISTOGRAM      | 234
        false    | "multiple" | "Long"    | "longHistogram"               | HISTOGRAM      | 234
        true     | "single"   | "Double"  | "doubleCounterCallback"       | DOUBLE_SUM   | 123.456
        false    | "multiple" | "Double"  | "doubleCounterCallback"       | DOUBLE_SUM   | 123.456
        true     | "single"   | "Double"  | "doubleUpDownCounterCallback" | DOUBLE_SUM   | 123.456
        false    | "multiple" | "Double"  | "doubleUpDownCounterCallback" | DOUBLE_SUM   | 123.456
        true     | "single"   | "Long"    | "longCounterCallback"         | LONG_SUM     | 234
        false    | "multiple" | "Long"    | "longCounterCallback"         | LONG_SUM     | 234
        true     | "single"   | "Long"    | "longUpDownCounterCallback"   | LONG_SUM     | 234
        false    | "multiple" | "Long"    | "longUpDownCounterCallback"   | LONG_SUM     | 234
        true     | "single"   | "Double"  | "doubleValueCallback"         | DOUBLE_GAUGE | 123.456
        false    | "multiple" | "Double"  | "doubleValueCallback"         | DOUBLE_GAUGE | 123.456
        true     | "single"   | "Long"    | "longValueCallback"           | LONG_GAUGE   | 234
        false    | "multiple" | "Long"    | "longValueCallback"           | LONG_GAUGE   | 234
    }

    @Unroll
    def "handles nulls returned from MBeanHelper"() {
        setup:
        "Create and register four Things and create ${quantity} MBeanHelper"
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
                ["labelOne": { "labelOneValue" }, "labelTwo": { mbean -> mbean.name().getKeyProperty("thing") }],
                attribute, instrument)
        instrumentHelper.update()

        then:
        def metrics = exportMetrics()
        metrics.size() == 0

        where:
        isSingle | quantity   | attribute | instrumentMethod    | metricType | value
        true     | "single"   | "Missing" | "longValueCallback" | LONG_GAUGE | null
        false    | "multiple" | "Missing" | "longValueCallback" | LONG_GAUGE | null
    }

    @Unroll
    def "#instrumentMethod correctly classified"() {
        expect:
        def instrument = otel.&"${instrumentMethod}"
        assert InstrumentHelper.instrumentIsObserver(instrument) == isObserver
        assert InstrumentHelper.instrumentIsCounter(instrument) == isCounter

        where:
        instrumentMethod              | isObserver | isCounter
        "doubleCounter"               | false      | true
        "longCounter"                 | false      | true
        "doubleCounterCallback"       | true       | false
        "longCounterCallback"         | true       | false
        "doubleUpDownCounter"         | false      | true
        "longUpDownCounter"           | false      | true
        "doubleUpDownCounterCallback" | true       | false
        "longUpDownCounterCallback"   | true       | false
        "doubleValueCallback"         | true       | false
        "longValueCallback"           | true       | false
        "doubleHistogram"             | false      | false
        "longHistogram"               | false      | false
    }
}
