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

import io.opentelemetry.api.common.Labels
import io.opentelemetry.sdk.OpenTelemetrySdk
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
                    it.setProperty(JmxConfig.EXPORTER_TYPE, 'inmemory')
                    it.setProperty(JmxConfig.INTERVAL_MILLISECONDS, '100')
                }),
                name.methodName, ''
                )
        otel = new OtelHelper(null, gme)
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

    def "double sum observer"() {
        when:
        def dso = otel.doubleSumObserver(
                'double-sum', 'a double sum',
                'ms')
        dso.setCallback({doubleResult ->
            doubleResult.observe(123.456, Labels.of('key', 'value'))
        })

        dso = otel.doubleSumObserver('my-double-sum', 'another double sum', 'µs')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(234.567, Labels.of('myKey', 'myValue'))
        } )

        dso = otel.doubleSumObserver('another-double-sum', 'double sum')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(345.678, Labels.of('anotherKey', 'anotherValue'))
        })

        dso = otel.doubleSumObserver('yet-another-double-sum')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(456.789, Labels.of('yetAnotherKey', 'yetAnotherValue'))
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
        assert first.type == MONOTONIC_DOUBLE
        assert first.points.size() == 1
        assert first.points[0].value == 123.456
        assert first.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-double-sum'
        assert second.description == 'another double sum'
        assert second.unit == 'µs'
        assert second.type == MONOTONIC_DOUBLE
        assert second.points.size() == 1
        assert second.points[0].value == 234.567
        assert second.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-double-sum'
        assert third.description == 'double sum'
        assert third.unit == '1'
        assert third.type == MONOTONIC_DOUBLE
        assert third.points.size() == 1
        assert third.points[0].value == 345.678
        assert third.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-double-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == MONOTONIC_DOUBLE
        assert fourth.points.size() == 1
        assert fourth.points[0].value == 456.789
        assert fourth.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "double sum observer memoization"() {
        when:
        def dcOne = otel.doubleSumObserver('dc', 'double')
        dcOne.setCallback({ doubleResult ->
            doubleResult.observe(10.1, Labels.of('key1', 'value1'))
        })
        def dcTwo = otel.doubleSumObserver('dc', 'double')
        dcTwo.setCallback({ doubleResult ->
            doubleResult.observe(20.2, Labels.of('key2', 'value2'))
        })
        def firstMetrics = exportMetrics()

        def dcThree = otel.doubleSumObserver('dc', 'double')
        dcOne.setCallback({ doubleResult ->
            doubleResult.observe(30.3, Labels.of('key3', 'value3'))
        })
        def dcFour = otel.doubleSumObserver('dc', 'double')
        dcTwo.setCallback({ doubleResult ->
            doubleResult.observe(40.4, Labels.of('key4', 'value4'))
        })
        def secondMetrics = exportMetrics()

        then:
        assert dcOne.is(dcTwo)
        assert dcTwo.is(dcThree)
        assert dcTwo.is(dcFour)

        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'double'
        assert firstMetric.unit == '1'
        assert firstMetric.type == MONOTONIC_DOUBLE
        assert firstMetric.points.size() == 1
        assert firstMetric.points[0].value == 20.2
        assert firstMetric.points[0].labels == Labels.of('key2', 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'double'
        assert secondMetric.unit == '1'
        assert secondMetric.type == MONOTONIC_DOUBLE
        assert secondMetric.points.size() == 2
        assert secondMetric.points[0].value == 20.2
        assert secondMetric.points[0].labels == Labels.of('key2', 'value2')
        assert secondMetric.points[1].value == 40.4
        assert secondMetric.points[1].labels == Labels.of('key4', 'value4')
    }

    def "long sum observer"() {
        when:
        def dso = otel.longSumObserver(
                'long-sum', 'a long sum',
                'ms')
        dso.setCallback({longResult ->
            longResult.observe(123, Labels.of('key', 'value'))
        })

        dso = otel.longSumObserver('my-long-sum', 'another long sum', 'µs')
        dso.setCallback({ longResult ->
            longResult.observe(234, Labels.of('myKey', 'myValue'))
        } )

        dso = otel.longSumObserver('another-long-sum', 'long sum')
        dso.setCallback({ longResult ->
            longResult.observe(345, Labels.of('anotherKey', 'anotherValue'))
        })

        dso = otel.longSumObserver('yet-another-long-sum')
        dso.setCallback({ longResult ->
            longResult.observe(456, Labels.of('yetAnotherKey', 'yetAnotherValue'))
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
        assert first.type == MONOTONIC_LONG
        assert first.points.size() == 1
        assert first.points[0].value == 123
        assert first.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-long-sum'
        assert second.description == 'another long sum'
        assert second.unit == 'µs'
        assert second.type == MONOTONIC_LONG
        assert second.points.size() == 1
        assert second.points[0].value == 234
        assert second.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-long-sum'
        assert third.description == 'long sum'
        assert third.unit == '1'
        assert third.type == MONOTONIC_LONG
        assert third.points.size() == 1
        assert third.points[0].value == 345
        assert third.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-long-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == MONOTONIC_LONG
        assert fourth.points.size() == 1
        assert fourth.points[0].value == 456
        assert fourth.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "long sum observer memoization"() {
        when:
        def dcOne = otel.longSumObserver('dc', 'long')
        dcOne.setCallback({ longResult ->
            longResult.observe(10, Labels.of('key1', 'value1'))
        })
        def dcTwo = otel.longSumObserver('dc', 'long')
        dcTwo.setCallback({ longResult ->
            longResult.observe(20, Labels.of('key2', 'value2'))
        })
        def firstMetrics = exportMetrics()

        def dcThree = otel.longSumObserver('dc', 'long')
        dcOne.setCallback({ longResult ->
            longResult.observe(30, Labels.of('key3', 'value3'))
        })
        def dcFour = otel.longSumObserver('dc', 'long')
        dcTwo.setCallback({ longResult ->
            longResult.observe(40, Labels.of('key4', 'value4'))
        })
        def secondMetrics = exportMetrics()

        then:
        assert dcOne.is(dcTwo)
        assert dcTwo.is(dcThree)
        assert dcTwo.is(dcFour)

        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'long'
        assert firstMetric.unit == '1'
        assert firstMetric.type == MONOTONIC_LONG
        assert firstMetric.points.size() == 1
        assert firstMetric.points[0].value == 20
        assert firstMetric.points[0].labels == Labels.of('key2', 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'long'
        assert secondMetric.unit == '1'
        assert secondMetric.type == MONOTONIC_LONG
        assert secondMetric.points.size() == 2
        assert secondMetric.points[0].value == 20
        assert secondMetric.points[0].labels == Labels.of('key2', 'value2')
        assert secondMetric.points[1].value == 40
        assert secondMetric.points[1].labels == Labels.of('key4', 'value4')
    }

    def "double up down sum observer"() {
        when:
        def dso = otel.doubleUpDownSumObserver(
                'double-up-down-sum', 'a double up down sum',
                'ms')
        dso.setCallback({doubleResult ->
            doubleResult.observe(123.456, Labels.of('key', 'value'))
        })

        dso = otel.doubleUpDownSumObserver('my-double-up-down-sum', 'another double up down sum', 'µs')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(234.567, Labels.of('myKey', 'myValue'))
        } )

        dso = otel.doubleUpDownSumObserver('another-double-up-down-sum', 'double up down sum')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(345.678, Labels.of('anotherKey', 'anotherValue'))
        })

        dso = otel.doubleUpDownSumObserver('yet-another-double-up-down-sum')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(456.789, Labels.of('yetAnotherKey', 'yetAnotherValue'))
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
        assert first.type == NON_MONOTONIC_DOUBLE
        assert first.points.size() == 1
        assert first.points[0].value == 123.456
        assert first.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-double-up-down-sum'
        assert second.description == 'another double up down sum'
        assert second.unit == 'µs'
        assert second.type == NON_MONOTONIC_DOUBLE
        assert second.points.size() == 1
        assert second.points[0].value == 234.567
        assert second.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-double-up-down-sum'
        assert third.description == 'double up down sum'
        assert third.unit == '1'
        assert third.type == NON_MONOTONIC_DOUBLE
        assert third.points.size() == 1
        assert third.points[0].value == 345.678
        assert third.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-double-up-down-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == NON_MONOTONIC_DOUBLE
        assert fourth.points.size() == 1
        assert fourth.points[0].value == 456.789
        assert fourth.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "double up down sum observer memoization"() {
        when:
        def dcOne = otel.doubleUpDownSumObserver('dc', 'double')
        dcOne.setCallback({ doubleResult ->
            doubleResult.observe(10.1, Labels.of('key1', 'value1'))
        })
        def dcTwo = otel.doubleUpDownSumObserver('dc', 'double')
        dcTwo.setCallback({ doubleResult ->
            doubleResult.observe(20.2, Labels.of('key2', 'value2'))
        })
        def firstMetrics = exportMetrics()

        def dcThree = otel.doubleUpDownSumObserver('dc', 'double')
        dcOne.setCallback({ doubleResult ->
            doubleResult.observe(30.3, Labels.of('key3', 'value3'))
        })
        def dcFour = otel.doubleUpDownSumObserver('dc', 'double')
        dcTwo.setCallback({ doubleResult ->
            doubleResult.observe(40.4, Labels.of('key4', 'value4'))
        })
        def secondMetrics = exportMetrics()

        then:
        assert dcOne.is(dcTwo)
        assert dcTwo.is(dcThree)
        assert dcTwo.is(dcFour)

        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'double'
        assert firstMetric.unit == '1'
        assert firstMetric.type == NON_MONOTONIC_DOUBLE
        assert firstMetric.points.size() == 1
        assert firstMetric.points[0].value == 20.2
        assert firstMetric.points[0].labels == Labels.of('key2', 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'double'
        assert secondMetric.unit == '1'
        assert secondMetric.type == NON_MONOTONIC_DOUBLE
        assert secondMetric.points.size() == 2
        assert secondMetric.points[0].value == 20.2
        assert secondMetric.points[0].labels == Labels.of('key2', 'value2')
        assert secondMetric.points[1].value == 40.4
        assert secondMetric.points[1].labels == Labels.of('key4', 'value4')
    }

    def "long up down sum observer"() {
        when:
        def dso = otel.longUpDownSumObserver(
                'long-up-down-sum', 'a long up down sum',
                'ms')
        dso.setCallback({longResult ->
            longResult.observe(123, Labels.of('key', 'value'))
        })

        dso = otel.longUpDownSumObserver('my-long-up-down-sum', 'another long up down sum', 'µs')
        dso.setCallback({ longResult ->
            longResult.observe(234, Labels.of('myKey', 'myValue'))
        } )

        dso = otel.longUpDownSumObserver('another-long-up-down-sum', 'long up down sum')
        dso.setCallback({ longResult ->
            longResult.observe(345, Labels.of('anotherKey', 'anotherValue'))
        })

        dso = otel.longUpDownSumObserver('yet-another-long-up-down-sum')
        dso.setCallback({ longResult ->
            longResult.observe(456, Labels.of('yetAnotherKey', 'yetAnotherValue'))
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
        assert first.type == NON_MONOTONIC_LONG
        assert first.points.size() == 1
        assert first.points[0].value == 123
        assert first.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-long-up-down-sum'
        assert second.description == 'another long up down sum'
        assert second.unit == 'µs'
        assert second.type == NON_MONOTONIC_LONG
        assert second.points.size() == 1
        assert second.points[0].value == 234
        assert second.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-long-up-down-sum'
        assert third.description == 'long up down sum'
        assert third.unit == '1'
        assert third.type == NON_MONOTONIC_LONG
        assert third.points.size() == 1
        assert third.points[0].value == 345
        assert third.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-long-up-down-sum'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == NON_MONOTONIC_LONG
        assert fourth.points.size() == 1
        assert fourth.points[0].value == 456
        assert fourth.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "long up down sum observer memoization"() {
        when:
        def dcOne = otel.longUpDownSumObserver('dc', 'long')
        dcOne.setCallback({ longResult ->
            longResult.observe(10, Labels.of('key1', 'value1'))
        })
        def dcTwo = otel.longUpDownSumObserver('dc', 'long')
        dcTwo.setCallback({ longResult ->
            longResult.observe(20, Labels.of('key2', 'value2'))
        })
        def firstMetrics = exportMetrics()

        def dcThree = otel.longUpDownSumObserver('dc', 'long')
        dcOne.setCallback({ longResult ->
            longResult.observe(30, Labels.of('key3', 'value3'))
        })
        def dcFour = otel.longUpDownSumObserver('dc', 'long')
        dcTwo.setCallback({ longResult ->
            longResult.observe(40, Labels.of('key4', 'value4'))
        })
        def secondMetrics = exportMetrics()

        then:
        assert dcOne.is(dcTwo)
        assert dcTwo.is(dcThree)
        assert dcTwo.is(dcFour)

        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'long'
        assert firstMetric.unit == '1'
        assert firstMetric.type == NON_MONOTONIC_LONG
        assert firstMetric.points.size() == 1
        assert firstMetric.points[0].value == 20
        assert firstMetric.points[0].labels == Labels.of('key2', 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'long'
        assert secondMetric.unit == '1'
        assert secondMetric.type == NON_MONOTONIC_LONG
        assert secondMetric.points.size() == 2
        assert secondMetric.points[0].value == 20
        assert secondMetric.points[0].labels == Labels.of('key2', 'value2')
        assert secondMetric.points[1].value == 40
        assert secondMetric.points[1].labels == Labels.of('key4', 'value4')
    }

    def "double value observer"() {
        when:
        def dso = otel.doubleValueObserver(
                'double-value', 'a double value',
                'ms')
        dso.setCallback({doubleResult ->
            doubleResult.observe(123.456, Labels.of('key', 'value'))
        })

        dso = otel.doubleValueObserver('my-double-value', 'another double value', 'µs')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(234.567, Labels.of('myKey', 'myValue'))
        } )

        dso = otel.doubleValueObserver('another-double-value', 'double value')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(345.678, Labels.of('anotherKey', 'anotherValue'))
        })

        dso = otel.doubleValueObserver('yet-another-double-value')
        dso.setCallback({ doubleResult ->
            doubleResult.observe(456.789, Labels.of('yetAnotherKey', 'yetAnotherValue'))
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
        assert first.type == GAUGE_DOUBLE
        assert first.points.size() == 1
        assert first.points[0].value == 123.456
        assert first.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-double-value'
        assert second.description == 'another double value'
        assert second.unit == 'µs'
        assert second.type == GAUGE_DOUBLE
        assert second.points[0].value == 234.567
        assert second.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-double-value'
        assert third.description == 'double value'
        assert third.unit == '1'
        assert third.type == GAUGE_DOUBLE
        assert third.points.size() == 1
        assert third.points[0].value == 345.678
        assert third.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-double-value'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == GAUGE_DOUBLE
        assert fourth.points.size() == 1
        assert fourth.points[0].value == 456.789
        assert fourth.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "double value observer memoization"() {
        when:
        def dcOne = otel.doubleValueObserver('dc', 'double')
        dcOne.setCallback({ doubleResult ->
            doubleResult.observe(10.1, Labels.of('key1', 'value1'))
        })
        def dcTwo = otel.doubleValueObserver('dc', 'double')
        dcTwo.setCallback({ doubleResult ->
            doubleResult.observe(20.2, Labels.of('key2', 'value2'))
        })
        def firstMetrics = exportMetrics()

        def dcThree = otel.doubleValueObserver('dc', 'double')
        dcOne.setCallback({ doubleResult ->
            doubleResult.observe(30.3, Labels.of('key3', 'value3'))
        })
        def dcFour = otel.doubleValueObserver('dc', 'double')
        dcTwo.setCallback({ doubleResult ->
            doubleResult.observe(40.4, Labels.of('key4', 'value4'))
            doubleResult.observe(50.5, Labels.of('key2', 'value2'))
        })
        def secondMetrics = exportMetrics()

        then:
        assert dcOne.is(dcTwo)
        assert dcTwo.is(dcThree)
        assert dcTwo.is(dcFour)

        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'double'
        assert firstMetric.unit == '1'
        assert firstMetric.type == GAUGE_DOUBLE
        assert firstMetric.points.size() == 1
        assert firstMetric.points[0].value == 20.2
        assert firstMetric.points[0].labels == Labels.of('key2', 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'double'
        assert secondMetric.unit == '1'
        assert secondMetric.type == GAUGE_DOUBLE
        assert secondMetric.points.size() == 2
        assert secondMetric.points[0].value == 40.4
        assert secondMetric.points[0].labels == Labels.of('key4', 'value4')
        assert secondMetric.points[1].value == 50.5
        assert secondMetric.points[1].labels == Labels.of('key2', 'value2')
    }

    def "long value observer"() {
        when:
        def dso = otel.longValueObserver(
                'long-value', 'a long value',
                'ms')
        dso.setCallback({longResult ->
            longResult.observe(123, Labels.of('key', 'value'))
        })

        dso = otel.longValueObserver('my-long-value', 'another long value', 'µs')
        dso.setCallback({ longResult ->
            longResult.observe(234, Labels.of('myKey', 'myValue'))
        } )

        dso = otel.longValueObserver('another-long-value', 'long value')
        dso.setCallback({ longResult ->
            longResult.observe(345, Labels.of('anotherKey', 'anotherValue'))
        })

        dso = otel.longValueObserver('yet-another-long-value')
        dso.setCallback({ longResult ->
            longResult.observe(456, Labels.of('yetAnotherKey', 'yetAnotherValue'))
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
        assert first.type == GAUGE_LONG
        assert first.points.size() == 1
        assert first.points[0].value == 123
        assert first.points[0].labels == Labels.of('key', 'value')

        assert second.name == 'my-long-value'
        assert second.description == 'another long value'
        assert second.unit == 'µs'
        assert second.type == GAUGE_LONG
        assert second.points.size() == 1
        assert second.points[0].value == 234
        assert second.points[0].labels == Labels.of('myKey', 'myValue')

        assert third.name == 'another-long-value'
        assert third.description == 'long value'
        assert third.unit == '1'
        assert third.type == GAUGE_LONG
        assert third.points.size() == 1
        assert third.points[0].value == 345
        assert third.points[0].labels == Labels.of('anotherKey', 'anotherValue')

        assert fourth.name == 'yet-another-long-value'
        assert fourth.description == ''
        assert fourth.unit == '1'
        assert fourth.type == GAUGE_LONG
        assert fourth.points.size() == 1
        assert fourth.points[0].value == 456
        assert fourth.points[0].labels == Labels.of('yetAnotherKey', 'yetAnotherValue')
    }

    def "long value observer memoization"() {
        when:
        def dcOne = otel.longValueObserver('dc', 'long')
        dcOne.setCallback({ longResult ->
            longResult.observe(10, Labels.of('key1', 'value1'))
        })
        def dcTwo = otel.longValueObserver('dc', 'long')
        dcTwo.setCallback({ longResult ->
            longResult.observe(20, Labels.of('key2', 'value2'))
        })
        def firstMetrics = exportMetrics()

        def dcThree = otel.longValueObserver('dc', 'long')
        dcOne.setCallback({ longResult ->
            longResult.observe(30, Labels.of('key3', 'value3'))
        })
        def dcFour = otel.longValueObserver('dc', 'long')
        dcTwo.setCallback({ longResult ->
            longResult.observe(40, Labels.of('key4', 'value4'))
            longResult.observe(50, Labels.of('key2', 'value2'))
        })
        def secondMetrics = exportMetrics()

        then:
        assert dcOne.is(dcTwo)
        assert dcTwo.is(dcThree)
        assert dcTwo.is(dcFour)

        assert firstMetrics.size() == 1
        assert secondMetrics.size() == 1

        def firstMetric = firstMetrics[0]
        assert firstMetric.name == 'dc'
        assert firstMetric.description == 'long'
        assert firstMetric.unit == '1'
        assert firstMetric.type == GAUGE_LONG
        assert firstMetric.points.size() == 1
        assert firstMetric.points[0].value == 20
        assert firstMetric.points[0].labels == Labels.of('key2', 'value2')

        def secondMetric = secondMetrics[0]
        assert secondMetric.name == 'dc'
        assert secondMetric.description == 'long'
        assert secondMetric.unit == '1'
        assert secondMetric.type == GAUGE_LONG
        assert secondMetric.points.size() == 2
        assert secondMetric.points[0].value == 40
        assert secondMetric.points[0].labels == Labels.of('key4', 'value4')
        assert secondMetric.points[1].value == 50
        assert secondMetric.points[1].labels == Labels.of('key2', 'value2')
    }
}
