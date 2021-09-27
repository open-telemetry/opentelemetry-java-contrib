/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import static io.opentelemetry.api.common.AttributeKey.stringKey
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_GAUGE
import static io.opentelemetry.sdk.metrics.data.MetricDataType.DOUBLE_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_GAUGE
import static io.opentelemetry.sdk.metrics.data.MetricDataType.LONG_SUM
import static io.opentelemetry.sdk.metrics.data.MetricDataType.SUMMARY

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.GlobalMeterProvider
import java.time.Clock
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Specification

class OtelHelperAsynchronousMetricTest extends Specification{

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
                    it.setProperty(JmxConfig.INTERVAL_MILLISECONDS, '100')
                }),
                name.methodName, ''
                )
        otel = new OtelHelper(null, gme)
    }

    def exportMetrics() {
        def now = Clock.systemUTC().instant();
        def nanos = TimeUnit.SECONDS.toNanos(now.epochSecond) + now.nano

        def provider = GlobalMeterProvider.get().get(name.methodName, '', null)
        def all = provider.collectAll(nanos)
        return all.sort { md1, md2 ->
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

    def "double sum observer"() {
        when:
        otel.doubleCounterCallback(
                'double-sum', 'a double sum',
                'ms', {doubleResult ->
                    doubleResult.observe(123.456, Attributes.builder().put('key', 'value').build())
                })

        otel.doubleCounterCallback('my-double-sum', 'another double sum', 'µs', { doubleResult ->
            doubleResult.observe(234.567, Attributes.builder().put('myKey', 'myValue').build())
        } )

        otel.doubleCounterCallback('another-double-sum', 'double sum', { doubleResult ->
            doubleResult.observe(345.678, Attributes.builder().put('anotherKey', 'anotherValue').build())
        })

        otel.doubleCounterCallback('yet-another-double-sum', { doubleResult ->
            doubleResult.observe(456.789, Attributes.builder().put('yetAnotherKey', 'yetAnotherValue').build())
        })

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'double-sum'
        assert first.description == 'a double sum'
        assert first.unit == 'ms'
        assert first.type == DOUBLE_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123.456
        assert first.data.points[0].attributes == Attributes.of(stringKey('key'), 'value')

        assert second.name == 'my-double-sum'
        assert second.description == 'another double sum'
        assert second.unit == 'µs'
        assert second.type == DOUBLE_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234.567
        assert second.data.points[0].attributes == Attributes.of(stringKey('myKey'), 'myValue')

        assert third.name == 'another-double-sum'
        assert third.description == 'double sum'
        assert third.unit == '1'
        assert third.type == DOUBLE_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345.678
        assert third.data.points[0].attributes == Attributes.of(stringKey('anotherKey'), 'anotherValue')

        assert fourth.name == 'yet-another-double-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == DOUBLE_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456.789
        assert fourth.data.points[0].attributes == Attributes.of(stringKey('yetAnotherKey'), 'yetAnotherValue')
    }

    def "double sum observer memoization"() {
        when:
        otel.doubleCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(10.1, Attributes.builder().put('key1', 'value1').build())
        })
        otel.doubleCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(20.2, Attributes.builder().put('key2', 'value2').build())
        })
        def firstMetrics = exportMetrics()

        otel.doubleCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(30.3, Attributes.builder().put('key3', 'value3').build())
        })
        otel.doubleCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(40.4, Attributes.builder().put('key4', 'value4').build())
        })
        def secondMetrics = exportMetrics()

        then:
        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'double'
        assert firstMetric.unit == '1'
        assert firstMetric.type == DOUBLE_SUM
        assert firstMetric.data.points.size() == 1
        assert firstMetric.data.points[0].value == 20.2
        assert firstMetric.data.points[0].attributes == Attributes.of(stringKey('key2'), 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'double'
        assert secondMetric.unit == '1'
        assert secondMetric.type == DOUBLE_SUM
        assert secondMetric.data.points.size() == 1
        assert secondMetric.data.points[0].value == 40.4
        assert secondMetric.data.points[0].attributes == Attributes.of(stringKey('key4'), 'value4')
    }

    def "long sum observer"() {
        when:
        otel.longCounterCallback(
                'long-sum', 'a long sum',
                'ms', {longResult ->
                    longResult.observe(123, Attributes.builder().put('key', 'value').build())
                })

        otel.longCounterCallback('my-long-sum', 'another long sum', 'µs', { longResult ->
            longResult.observe(234, Attributes.builder().put('myKey', 'myValue').build())
        } )

        otel.longCounterCallback('another-long-sum', 'long sum', { longResult ->
            longResult.observe(345, Attributes.builder().put('anotherKey', 'anotherValue').build())
        })

        otel.longCounterCallback('yet-another-long-sum', { longResult ->
            longResult.observe(456, Attributes.builder().put('yetAnotherKey', 'yetAnotherValue').build())
        })

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'long-sum'
        assert first.description == 'a long sum'
        assert first.unit == 'ms'
        assert first.type == LONG_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123
        assert first.data.points[0].attributes == Attributes.of(stringKey('key'), 'value')

        assert second.name == 'my-long-sum'
        assert second.description == 'another long sum'
        assert second.unit == 'µs'
        assert second.type == LONG_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234
        assert second.data.points[0].attributes == Attributes.of(stringKey('myKey'), 'myValue')

        assert third.name == 'another-long-sum'
        assert third.description == 'long sum'
        assert third.unit == '1'
        assert third.type == LONG_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345
        assert third.data.points[0].attributes == Attributes.of(stringKey('anotherKey'), 'anotherValue')

        assert fourth.name == 'yet-another-long-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == LONG_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456
        assert fourth.data.points[0].attributes == Attributes.of(stringKey('yetAnotherKey'), 'yetAnotherValue')
    }

    def "long sum observer memoization"() {
        when:
        otel.longCounterCallback('dc', 'long', { longResult ->
            longResult.observe(10, Attributes.builder().put('key1', 'value1').build())
        })
        otel.longCounterCallback('dc', 'long', { longResult ->
            longResult.observe(20, Attributes.builder().put('key2', 'value2').build())
        })
        def firstMetrics = exportMetrics()

        otel.longCounterCallback('dc', 'long', { longResult ->
            longResult.observe(30, Attributes.builder().put('key3', 'value3').build())
        })
        otel.longCounterCallback('dc', 'long', { longResult ->
            longResult.observe(40, Attributes.builder().put('key4', 'value4').build())
        })
        def secondMetrics = exportMetrics()

        then:
        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'long'
        assert firstMetric.unit == '1'
        assert firstMetric.type == LONG_SUM
        assert firstMetric.data.points.size() == 1
        assert firstMetric.data.points[0].value == 20
        assert firstMetric.data.points[0].attributes == Attributes.of(stringKey('key2'), 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'long'
        assert secondMetric.unit == '1'
        assert secondMetric.type == LONG_SUM
        assert secondMetric.data.points.size() == 1
        assert secondMetric.data.points[0].value == 40
        assert secondMetric.data.points[0].attributes == Attributes.of(stringKey('key4'), 'value4')
    }

    def "double up down sum observer"() {
        when:
        otel.doubleUpDownCounterCallback(
                'double-up-down-sum', 'a double up down sum',
                'ms', {doubleResult ->
                    doubleResult.observe(123.456, Attributes.builder().put('key', 'value').build())
                })

        otel.doubleUpDownCounterCallback('my-double-up-down-sum', 'another double up down sum', 'µs', { doubleResult ->
            doubleResult.observe(234.567, Attributes.builder().put('myKey', 'myValue').build())
        } )

        otel.doubleUpDownCounterCallback('another-double-up-down-sum', 'double up down sum', { doubleResult ->
            doubleResult.observe(345.678, Attributes.builder().put('anotherKey', 'anotherValue').build())
        })

        otel.doubleUpDownCounterCallback('yet-another-double-up-down-sum', { doubleResult ->
            doubleResult.observe(456.789, Attributes.builder().put('yetAnotherKey', 'yetAnotherValue').build())
        })

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'double-up-down-sum'
        assert first.description == 'a double up down sum'
        assert first.unit == 'ms'
        assert first.type == DOUBLE_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123.456
        assert first.data.points[0].attributes == Attributes.of(stringKey('key'), 'value')

        assert second.name == 'my-double-up-down-sum'
        assert second.description == 'another double up down sum'
        assert second.unit == 'µs'
        assert second.type == DOUBLE_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234.567
        assert second.data.points[0].attributes == Attributes.of(stringKey('myKey'), 'myValue')

        assert third.name == 'another-double-up-down-sum'
        assert third.description == 'double up down sum'
        assert third.unit == '1'
        assert third.type == DOUBLE_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345.678
        assert third.data.points[0].attributes == Attributes.of(stringKey('anotherKey'), 'anotherValue')

        assert fourth.name == 'yet-another-double-up-down-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == DOUBLE_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456.789
        assert fourth.data.points[0].attributes == Attributes.of(stringKey('yetAnotherKey'), 'yetAnotherValue')
    }

    def "double up down sum observer memoization"() {
        when:
        otel.doubleUpDownCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(10.1, Attributes.builder().put('key1', 'value1').build())
        })
        otel.doubleUpDownCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(20.2, Attributes.builder().put('key2', 'value2').build())
        })
        def firstMetrics = exportMetrics()

        otel.doubleUpDownCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(30.3, Attributes.builder().put('key3', 'value3').build())
        })
        otel.doubleUpDownCounterCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(40.4, Attributes.builder().put('key4', 'value4').build())
        })
        def secondMetrics = exportMetrics()

        then:
        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'double'
        assert firstMetric.unit == '1'
        assert firstMetric.type == DOUBLE_SUM
        assert firstMetric.data.points.size() == 1
        assert firstMetric.data.points[0].value == 20.2
        assert firstMetric.data.points[0].attributes == Attributes.of(stringKey('key2'), 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'double'
        assert secondMetric.unit == '1'
        assert secondMetric.type == DOUBLE_SUM
        assert secondMetric.data.points.size() == 1
        assert secondMetric.data.points[0].value == 40.4
        assert secondMetric.data.points[0].attributes == Attributes.of(stringKey('key4'), 'value4')
    }

    def "long up down sum observer"() {
        when:
        otel.longUpDownCounterCallback(
                'long-up-down-sum', 'a long up down sum',
                'ms', {longResult ->
                    longResult.observe(123, Attributes.builder().put('key', 'value').build())
                })

        otel.longUpDownCounterCallback('my-long-up-down-sum', 'another long up down sum', 'µs', { longResult ->
            longResult.observe(234, Attributes.builder().put('myKey', 'myValue').build())
        } )

        otel.longUpDownCounterCallback('another-long-up-down-sum', 'long up down sum', { longResult ->
            longResult.observe(345, Attributes.builder().put('anotherKey', 'anotherValue').build())
        })

        otel.longUpDownCounterCallback('yet-another-long-up-down-sum', { longResult ->
            longResult.observe(456, Attributes.builder().put('yetAnotherKey', 'yetAnotherValue').build())
        })

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'long-up-down-sum'
        assert first.description == 'a long up down sum'
        assert first.unit == 'ms'
        assert first.type == LONG_SUM
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123
        assert first.data.points[0].attributes == Attributes.of(stringKey('key'), 'value')

        assert second.name == 'my-long-up-down-sum'
        assert second.description == 'another long up down sum'
        assert second.unit == 'µs'
        assert second.type == LONG_SUM
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234
        assert second.data.points[0].attributes == Attributes.of(stringKey('myKey'), 'myValue')

        assert third.name == 'another-long-up-down-sum'
        assert third.description == 'long up down sum'
        assert third.unit == '1'
        assert third.type == LONG_SUM
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345
        assert third.data.points[0].attributes == Attributes.of(stringKey('anotherKey'), 'anotherValue')

        assert fourth.name == 'yet-another-long-up-down-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == LONG_SUM
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456
        assert fourth.data.points[0].attributes == Attributes.of(stringKey('yetAnotherKey'), 'yetAnotherValue')
    }

    def "long up down sum observer memoization"() {
        when:
        otel.longUpDownCounterCallback('dc', 'long', { longResult ->
            longResult.observe(10, Attributes.builder().put('key1', 'value1').build())
        })
        otel.longUpDownCounterCallback('dc', 'long', { longResult ->
            longResult.observe(20, Attributes.builder().put('key2', 'value2').build())
        })
        def firstMetrics = exportMetrics()

        otel.longUpDownCounterCallback('dc', 'long', { longResult ->
            longResult.observe(30, Attributes.builder().put('key3', 'value3').build())
        })
        otel.longUpDownCounterCallback('dc', 'long', { longResult ->
            longResult.observe(40, Attributes.builder().put('key4', 'value4').build())
        })
        def secondMetrics = exportMetrics()

        then:
        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'long'
        assert firstMetric.unit == '1'
        assert firstMetric.type == LONG_SUM
        assert firstMetric.data.points.size() == 1
        assert firstMetric.data.points[0].value == 20
        assert firstMetric.data.points[0].attributes == Attributes.of(stringKey('key2'), 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'long'
        assert secondMetric.unit == '1'
        assert secondMetric.type == LONG_SUM
        assert secondMetric.data.points.size() == 1
        assert secondMetric.data.points[0].value == 40
        assert secondMetric.data.points[0].attributes == Attributes.of(stringKey('key4'), 'value4')
    }

    def "double value observer"() {
        when:
        otel.doubleValueCallback(
                'double-value', 'a double value',
                'ms', {doubleResult ->
                    doubleResult.observe(123.456, Attributes.builder().put('key', 'value').build())
                })

        otel.doubleValueCallback('my-double-value', 'another double value', 'µs', { doubleResult ->
            doubleResult.observe(234.567, Attributes.builder().put('myKey', 'myValue').build())
        } )

        otel.doubleValueCallback('another-double-value', 'double value', { doubleResult ->
            doubleResult.observe(345.678, Attributes.builder().put('anotherKey', 'anotherValue').build())
        })

        otel.doubleValueCallback('yet-another-double-value', { doubleResult ->
            doubleResult.observe(456.789, Attributes.builder().put('yetAnotherKey', 'yetAnotherValue').build())
        })

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'double-value'
        assert first.description == 'a double value'
        assert first.unit == 'ms'
        assert first.type == DOUBLE_GAUGE
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123.456
        assert first.data.points[0].attributes == Attributes.of(stringKey('key'), 'value')

        assert second.name == 'my-double-value'
        assert second.description == 'another double value'
        assert second.unit == 'µs'
        assert second.type == DOUBLE_GAUGE
        assert second.data.points[0].value == 234.567
        assert second.data.points[0].attributes == Attributes.of(stringKey('myKey'), 'myValue')

        assert third.name == 'another-double-value'
        assert third.description == 'double value'
        assert third.unit == '1'
        assert third.type == DOUBLE_GAUGE
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345.678
        assert third.data.points[0].attributes == Attributes.of(stringKey('anotherKey'), 'anotherValue')

        assert fourth.name == 'yet-another-double-value'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == DOUBLE_GAUGE
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456.789
        assert fourth.data.points[0].attributes == Attributes.of(stringKey('yetAnotherKey'), 'yetAnotherValue')
    }

    def "double value observer memoization"() {
        when:
        otel.doubleValueCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(10.1, Attributes.builder().put('key1', 'value1').build())
        })
        otel.doubleValueCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(20.2, Attributes.builder().put('key2', 'value2').build())
        })
        def firstMetrics = exportMetrics()

        otel.doubleValueCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(30.3, Attributes.builder().put('key3', 'value3').build())
        })
        otel.doubleValueCallback('dc', 'double', { doubleResult ->
            doubleResult.observe(40.4, Attributes.builder().put('key4', 'value4').build())
            doubleResult.observe(50.5, Attributes.builder().put('key2', 'value2').build())
        })
        def secondMetrics = exportMetrics()

        then:
        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'double'
        assert firstMetric.unit == '1'
        assert firstMetric.type == DOUBLE_GAUGE
        assert firstMetric.data.points.size() == 1
        assert firstMetric.data.points[0].value == 20.2
        assert firstMetric.data.points[0].attributes == Attributes.of(stringKey('key2'), 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'double'
        assert secondMetric.unit == '1'
        assert secondMetric.type == DOUBLE_GAUGE
        assert secondMetric.data.points.size() == 2
        // Same epochNanos, order not defined
        secondMetric.data.points.sort { a, b -> Double.compare(a.value, b.value) }
        assert secondMetric.data.points[0].value == 40.4
        assert secondMetric.data.points[0].attributes == Attributes.of(stringKey('key4'), 'value4')
        assert secondMetric.data.points[1].value == 50.5
        assert secondMetric.data.points[1].attributes == Attributes.of(stringKey('key2'), 'value2')
    }

    def "long value observer"() {
        when:
        otel.longValueCallback(
                'long-value', 'a long value',
                'ms', {longResult ->
                    longResult.observe(123, Attributes.builder().put('key', 'value').build())
                })

        otel.longValueCallback('my-long-value', 'another long value', 'µs', { longResult ->
            longResult.observe(234, Attributes.builder().put('myKey', 'myValue').build())
        } )

        otel.longValueCallback('another-long-value', 'long value', { longResult ->
            longResult.observe(345, Attributes.builder().put('anotherKey', 'anotherValue').build())
        })

        otel.longValueCallback('yet-another-long-value', { longResult ->
            longResult.observe(456, Attributes.builder().put('yetAnotherKey', 'yetAnotherValue').build())
        })

        def metrics = exportMetrics()
        then:
        assert metrics.size() == 4

        def first = metrics[0]
        def second = metrics[1]
        def third = metrics[2]
        def fourth = metrics[3]

        assert first.name == 'long-value'
        assert first.description == 'a long value'
        assert first.unit == 'ms'
        assert first.type == LONG_GAUGE
        assert first.data.points.size() == 1
        assert first.data.points[0].value == 123
        assert first.data.points[0].attributes == Attributes.of(stringKey('key'), 'value')

        assert second.name == 'my-long-value'
        assert second.description == 'another long value'
        assert second.unit == 'µs'
        assert second.type == LONG_GAUGE
        assert second.data.points.size() == 1
        assert second.data.points[0].value == 234
        assert second.data.points[0].attributes == Attributes.of(stringKey('myKey'), 'myValue')

        assert third.name == 'another-long-value'
        assert third.description == 'long value'
        assert third.unit == '1'
        assert third.type == LONG_GAUGE
        assert third.data.points.size() == 1
        assert third.data.points[0].value == 345
        assert third.data.points[0].attributes == Attributes.of(stringKey('anotherKey'), 'anotherValue')

        assert fourth.name == 'yet-another-long-value'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == LONG_GAUGE
        assert fourth.data.points.size() == 1
        assert fourth.data.points[0].value == 456
        assert fourth.data.points[0].attributes == Attributes.of(stringKey('yetAnotherKey'), 'yetAnotherValue')
    }

    def "long value observer memoization"() {
        when:
        otel.longValueCallback('dc', 'long', { longResult ->
            longResult.observe(10, Attributes.builder().put('key1', 'value1').build())
        })
        otel.longValueCallback('dc', 'long', { longResult ->
            longResult.observe(20, Attributes.builder().put('key2', 'value2').build())
        })
        def firstMetrics = exportMetrics()

        otel.longValueCallback('dc', 'long', { longResult ->
            longResult.observe(30, Attributes.builder().put('key3', 'value3').build())
        })
        otel.longValueCallback('dc', 'long', { longResult ->
            longResult.observe(40, Attributes.builder().put('key4', 'value4').build())
            longResult.observe(50, Attributes.builder().put('key2', 'value2').build())
        })
        def secondMetrics = exportMetrics()

        then:
        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'long'
        assert firstMetric.unit == '1'
        assert firstMetric.type == LONG_GAUGE
        assert firstMetric.data.points.size() == 1
        assert firstMetric.data.points[0].value == 20
        assert firstMetric.data.points[0].attributes == Attributes.of(stringKey('key2'), 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'long'
        assert secondMetric.unit == '1'
        assert secondMetric.type == LONG_GAUGE
        assert secondMetric.data.points.size() == 2
        // Same epochNanos, order not defined
        secondMetric.data.points.sort { a, b -> Long.compare(a.value, b.value) }
        assert secondMetric.data.points[0].value == 40
        assert secondMetric.data.points[0].attributes == Attributes.of(stringKey('key4'), 'value4')
        assert secondMetric.data.points[1].value == 50
        assert secondMetric.data.points[1].attributes == Attributes.of(stringKey('key2'), 'value2')
    }
}
