/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.SUMMARY

import io.opentelemetry.api.metrics.common.Labels
import io.opentelemetry.api.metrics.GlobalMeterProvider
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.rules.TestRule

import spock.lang.Shared
import spock.lang.Specification

class OtelHelperSynchronousMetricTest extends Specification{

    @Shared
    GroovyMetricEnvironment gme

    @Shared
    OtelHelper otel

    @Rule public final TestRule name = new TestName()

    def setup() {
        // Set up a MeterSdk per test to be able to collect its metrics alone
        gme = new GroovyMetricEnvironment(
                new JmxConfig(new Properties().tap {
                    it.setProperty(JmxConfig.METRICS_EXPORTER_TYPE, 'inmemory')
                }),
                name.methodName, ''
                )
        otel = new OtelHelper(null, gme)
    }

    def exportMetrics() {
        def provider = GlobalMeterProvider.get().get(name.methodName, '')
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

    def "double counter"() {
        when:
        def dc = otel.doubleCounter(
                'double-counter', 'a double counter',
                'ms')
        dc.add(123.456, Labels.of('key', 'value'))

        dc = otel.doubleCounter('my-double-counter', 'another double counter', 'µs')
        dc.add(234.567, Labels.of('myKey', 'myValue'))

        dc = otel.doubleCounter('another-double-counter', 'double counter')
        dc.add(345.678, Labels.of('anotherKey', 'anotherValue'))

        dc = otel.doubleCounter('yet-another-double-counter')
        dc.add(456.789, Labels.of('yetAnotherKey', 'yetAnotherValue'))

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'double-counter'
        assert first.description == 'a double counter'
        assert first.unit == 'ms'
        assert first.type == DOUBLE_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123.456
        assert first.data.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-double-counter'
        assert second.description == 'another double counter'
        assert second.unit == 'µs'
        assert second.type == DOUBLE_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234.567
        assert second.data.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-double-counter'
        assert third.description == 'double counter'
        assert third.unit == '1'
        assert third.type == DOUBLE_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345.678
        assert third.data.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-double-counter'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == DOUBLE_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456.789
        assert fourth.data.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "double counter memoization"() {
        when:
        def dcOne = otel.doubleCounter('dc', 'double')
        dcOne.add(10.1, Labels.of('key', 'value'))
        def dcTwo = otel.doubleCounter('dc', 'double')
        dcTwo.add(10.1, Labels.of('key', 'value'))

        then:
        assert dcOne.is(dcTwo)

        def metrics = exportMetrics()
        assert metrics.size() == 1
        def metric = metrics[0]

        assert metric.name == 'dc'
        assert metric.description == 'double'
        assert metric.unit == '1'
        assert metric.type == DOUBLE_SUM
        assert metric.data.points.size() == 1
        assert metric.data.points[0].value == 20.2
        assert metric.data.points[0].labels == Labels.of('key', 'value')
    }

    def "long counter"() {
        when:
        def lc = otel.longCounter(
                'long-counter', 'a long counter',
                'ms')
        lc.add(123, Labels.of('key', 'value'))

        lc = otel.longCounter('my-long-counter', 'another long counter', 'µs')
        lc.add(234, Labels.of('myKey', 'myValue'))

        lc = otel.longCounter('another-long-counter', 'long counter')
        lc.add(345, Labels.of('anotherKey', 'anotherValue'))

        lc = otel.longCounter('yet-another-long-counter')
        lc.add(456, Labels.of('yetAnotherKey', 'yetAnotherValue'))

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'long-counter'
        assert first.description == 'a long counter'
        assert first.unit == 'ms'
        assert first.type == LONG_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123
        assert first.data.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-long-counter'
        assert second.description == 'another long counter'
        assert second.unit == 'µs'
        assert second.type == LONG_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234
        assert second.data.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-long-counter'
        assert third.description == 'long counter'
        assert third.unit == '1'
        assert third.type == LONG_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345
        assert third.data.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-long-counter'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == LONG_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456
        assert fourth.data.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "long counter memoization"() {
        when:
        def lcOne = otel.longCounter('lc', 'long')
        lcOne.add(10, Labels.of('key', 'value'))
        def lcTwo = otel.longCounter('lc', 'long')
        lcTwo.add(10, Labels.of('key', 'value'))

        then:
        assert lcOne.is(lcTwo)

        def metrics = exportMetrics()
        assert metrics.size() == 1
        def metric = metrics[0]

        assert metric.name == 'lc'
        assert metric.description == 'long'
        assert metric.unit == '1'
        assert metric.type == LONG_SUM
        assert metric.data.points.size() == 1
        assert metric.data.points[0].value == 20
        assert metric.data.points[0].labels == Labels.of('key', 'value')
    }

    def "double up down counter"() {
        when:
        def dudc = otel.doubleUpDownCounter(
                'double-up-down-counter', 'a double up-down-counter',
                'ms')
        dudc.add(-234.567, Labels.of('key', 'value'))

        dudc = otel.doubleUpDownCounter('my-double-up-down-counter', 'another double up-down-counter', 'µs')
        dudc.add(-123.456, Labels.of('myKey', 'myValue'))

        dudc = otel.doubleUpDownCounter('another-double-up-down-counter', 'double up-down-counter')
        dudc.add(345.678, Labels.of('anotherKey', 'anotherValue'))

        dudc = otel.doubleUpDownCounter('yet-another-double-up-down-counter')
        dudc.add(456.789, Labels.of('yetAnotherKey', 'yetAnotherValue'))

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'double-up-down-counter'
        assert first.description == 'a double up-down-counter'
        assert first.unit == 'ms'
        assert first.type == DOUBLE_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == -234.567
        assert first.data.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-double-up-down-counter'
        assert second.description == 'another double up-down-counter'
        assert second.unit == 'µs'
        assert second.type == DOUBLE_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == -123.456
        assert second.data.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-double-up-down-counter'
        assert third.description == 'double up-down-counter'
        assert third.unit == '1'
        assert third.type == DOUBLE_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345.678
        assert third.data.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-double-up-down-counter'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == DOUBLE_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456.789
        assert fourth.data.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "double up down counter memoization"() {
        when:
        def dudcOne = otel.doubleUpDownCounter('dudc', 'double up down')
        dudcOne.add(10.1, Labels.of('key', 'value'))
        def dudcTwo = otel.doubleUpDownCounter('dudc', 'double up down')
        dudcTwo.add(-10.1, Labels.of('key', 'value'))

        then:
        assert dudcOne.is(dudcTwo)

        def metrics = exportMetrics()
        assert metrics.size() == 1
        def metric = metrics[0]

        assert metric.name == 'dudc'
        assert metric.description == 'double up down'
        assert metric.unit == '1'
        assert metric.type == DOUBLE_SUM
        assert metric.data.points.size() == 1
        assert metric.data.points[0].value == 0
        assert metric.data.points[0].labels == Labels.of('key', 'value')
    }

    def "long up down counter"() {
        when:
        def ludc = otel.longUpDownCounter(
                'long-up-down-counter', 'a long up-down-counter',
                'ms')
        ludc.add(-234, Labels.of('key', 'value'))

        ludc = otel.longUpDownCounter('my-long-up-down-counter', 'another long up-down-counter', 'µs')
        ludc.add(-123, Labels.of('myKey', 'myValue'))

        ludc = otel.longUpDownCounter('another-long-up-down-counter', 'long up-down-counter')
        ludc.add(345, Labels.of('anotherKey', 'anotherValue'))

        ludc = otel.longUpDownCounter('yet-another-long-up-down-counter')
        ludc.add(456, Labels.of('yetAnotherKey', 'yetAnotherValue'))

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'long-up-down-counter'
        assert first.description == 'a long up-down-counter'
        assert first.unit == 'ms'
        assert first.type == LONG_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == -234
        assert first.data.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-long-up-down-counter'
        assert second.description == 'another long up-down-counter'
        assert second.unit == 'µs'
        assert second.type == LONG_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == -123
        assert second.data.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-long-up-down-counter'
        assert third.description == 'long up-down-counter'
        assert third.unit == '1'
        assert third.type == LONG_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345
        assert third.data.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-long-up-down-counter'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == LONG_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456
        assert fourth.data.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "long up down counter memoization"() {
        when:
        def ludcOne = otel.longUpDownCounter('ludc', 'long up down')
        ludcOne.add(10, Labels.of('key', 'value'))
        def ludcTwo = otel.longUpDownCounter('ludc', 'long up down')
        ludcTwo.add(-10, Labels.of('key', 'value'))

        then:
        assert ludcOne.is(ludcTwo)

        def metrics = exportMetrics()
        assert metrics.size() == 1
        def metric = metrics[0]

        assert metric.name == 'ludc'
        assert metric.description == 'long up down'
        assert metric.unit == '1'
        assert metric.type == LONG_SUM
        assert metric.data.points.size() == 1
        assert metric.data.points[0].value == 0
        assert metric.data.points[0].labels == Labels.of('key', 'value')
    }

    def "double value recorder"() {
        when:
        def dvr = otel.doubleValueRecorder(
                'double-value-recorder', 'a double value-recorder',
                'ms')
        dvr.record(-234.567, Labels.of('key', 'value'))

        dvr = otel.doubleValueRecorder('my-double-value-recorder', 'another double value-recorder', 'µs')
        dvr.record(-123.456, Labels.of('myKey', 'myValue'))

        dvr = otel.doubleValueRecorder('another-double-value-recorder', 'double value-recorder')
        dvr.record(345.678, Labels.of('anotherKey', 'anotherValue'))

        dvr = otel.doubleValueRecorder('yet-another-double-value-recorder')
        dvr.record(456.789, Labels.of('yetAnotherKey', 'yetAnotherValue'))

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'double-value-recorder'
        assert first.description == 'a double value-recorder'
        assert first.unit == 'ms'
        assert first.type == SUMMARY
        assert first.data.points.size() == 1
        assert first.data.points[0].count == 1
        assert first.data.points[0].sum == -234.567
        assert first.data.points[0].percentileValues[0].percentile == 0
        assert first.data.points[0].percentileValues[0].value ==  -234.567
        assert first.data.points[0].percentileValues[1].percentile == 100
        assert first.data.points[0].percentileValues[1].value == -234.567
        assert first.data.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-double-value-recorder'
        assert second.description == 'another double value-recorder'
        assert second.unit == 'µs'
        assert second.type == SUMMARY
        assert second.data.points.size() == 1
        assert second.data.points[0].count == 1
        assert second.data.points[0].sum == -123.456
        assert second.data.points[0].percentileValues[0].percentile == 0
        assert second.data.points[0].percentileValues[0].value == -123.456
        assert second.data.points[0].percentileValues[1].percentile == 100
        assert second.data.points[0].percentileValues[1].value == -123.456
        assert second.data.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-double-value-recorder'
        assert third.description == 'double value-recorder'
        assert third.unit == '1'
        assert third.type == SUMMARY
        assert third.data.points.size() == 1
        assert third.data.points[0].count == 1
        assert third.data.points[0].sum == 345.678
        assert third.data.points[0].percentileValues[0].percentile == 0
        assert third.data.points[0].percentileValues[0].value == 345.678
        assert third.data.points[0].percentileValues[1].percentile == 100
        assert third.data.points[0].percentileValues[1].value == 345.678
        assert third.data.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-double-value-recorder'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == SUMMARY
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].count == 1
        assert fourth.data.points[0].sum == 456.789
        assert fourth.data.points[0].percentileValues[0].percentile == 0
        assert fourth.data.points[0].percentileValues[0].value == 456.789
        assert fourth.data.points[0].percentileValues[1].percentile == 100
        assert fourth.data.points[0].percentileValues[1].value == 456.789
        assert fourth.data.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "double value recorder memoization"() {
        when:
        def dvrOne = otel.doubleValueRecorder('dvr', 'double value')
        dvrOne.record(10.1, Labels.of('key', 'value'))
        def dvrTwo = otel.doubleValueRecorder('dvr', 'double value')
        dvrTwo.record(-10.1, Labels.of('key', 'value'))

        then:
        assert dvrOne.is(dvrTwo)

        def metrics = exportMetrics()
        assert metrics.size() == 1
        def metric = metrics[0]

        assert metric.name == 'dvr'
        assert metric.description == 'double value'
        assert metric.unit == '1'
        assert metric.type == SUMMARY
        assert metric.data.points.size() == 1
        assert metric.data.points[0].count == 2
        assert metric.data.points[0].sum == 0
        assert metric.data.points[0].percentileValues[0].percentile == 0
        assert metric.data.points[0].percentileValues[0].value == -10.1
        assert metric.data.points[0].percentileValues[1].percentile == 100
        assert metric.data.points[0].percentileValues[1].value == 10.1
        assert metric.data.points[0].labels == Labels.of('key', 'value')
    }

    def "long value recorder"() {
        when:
        def lvr = otel.longValueRecorder(
                'long-value-recorder', 'a long value-recorder',
                'ms')
        lvr.record(-234, Labels.of('key', 'value'))

        lvr = otel.longValueRecorder('my-long-value-recorder', 'another long value-recorder', 'µs')
        lvr.record(-123, Labels.of('myKey', 'myValue'))

        lvr = otel.longValueRecorder('another-long-value-recorder', 'long value-recorder')
        lvr.record(345, Labels.of('anotherKey', 'anotherValue'))

        lvr = otel.longValueRecorder('yet-another-long-value-recorder')
        lvr.record(456, Labels.of('yetAnotherKey', 'yetAnotherValue'))

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'long-value-recorder'
        assert first.description == 'a long value-recorder'
        assert first.unit == 'ms'
        assert first.type == SUMMARY
        assert first.data.points.size() == 1
        assert first.data.points[0].count == 1
        assert first.data.points[0].sum == -234
        assert first.data.points[0].percentileValues[0].percentile == 0
        assert first.data.points[0].percentileValues[0].value == -234
        assert first.data.points[0].percentileValues[1].percentile == 100
        assert first.data.points[0].percentileValues[1].value == -234
        assert first.data.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-long-value-recorder'
        assert second.description == 'another long value-recorder'
        assert second.unit == 'µs'
        assert second.type == SUMMARY
        assert second.data.points.size() == 1
        assert second.data.points[0].count == 1
        assert second.data.points[0].sum == -123
        assert second.data.points[0].percentileValues[0].percentile == 0
        assert second.data.points[0].percentileValues[0].value == -123
        assert second.data.points[0].percentileValues[1].percentile == 100
        assert second.data.points[0].percentileValues[1].value == -123
        assert second.data.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-long-value-recorder'
        assert third.description == 'long value-recorder'
        assert third.unit == '1'
        assert third.type == SUMMARY
        assert third.data.points.size() == 1
        assert third.data.points[0].count == 1
        assert third.data.points[0].sum == 345
        assert third.data.points[0].percentileValues[0].percentile == 0
        assert third.data.points[0].percentileValues[0].value == 345
        assert third.data.points[0].percentileValues[1].percentile == 100
        assert third.data.points[0].percentileValues[1].value == 345
        assert third.data.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-long-value-recorder'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == SUMMARY
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].count == 1
        assert fourth.data.points[0].sum == 456
        assert fourth.data.points[0].percentileValues[0].percentile == 0
        assert fourth.data.points[0].percentileValues[0].value == 456
        assert fourth.data.points[0].percentileValues[1].percentile == 100
        assert fourth.data.points[0].percentileValues[1].value == 456
        assert fourth.data.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "long value recorder memoization"() {
        when:
        def lvrOne = otel.longValueRecorder('lvr', 'long value')
        lvrOne.record(10, Labels.of('key', 'value'))
        def lvrTwo = otel.longValueRecorder('lvr', 'long value')
        lvrTwo.record(-10, Labels.of('key', 'value'))

        then:
        assert lvrOne.is(lvrTwo)

        def metrics = exportMetrics()
        assert metrics.size() == 1
        def metric = metrics[0]

        assert metric.name == 'lvr'
        assert metric.description == 'long value'
        assert metric.unit == '1'
        assert metric.type == SUMMARY
        assert metric.data.points.size() == 1
        assert metric.data.points[0].count == 2
        assert metric.data.points[0].sum == 0
        assert metric.data.points[0].percentileValues[0].percentile == 0
        assert metric.data.points[0].percentileValues[0].value == -10
        assert metric.data.points[0].percentileValues[1].percentile == 100
        assert metric.data.points[0].percentileValues[1].value == 10
        assert metric.data.points[0].labels == Labels.of('key', 'value')
    }

}
