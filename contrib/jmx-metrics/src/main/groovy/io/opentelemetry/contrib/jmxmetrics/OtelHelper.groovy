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

import io.opentelemetry.metrics.DoubleCounter
import io.opentelemetry.metrics.DoubleSumObserver
import io.opentelemetry.metrics.DoubleUpDownCounter
import io.opentelemetry.metrics.DoubleUpDownSumObserver
import io.opentelemetry.metrics.DoubleValueObserver
import io.opentelemetry.metrics.DoubleValueRecorder
import io.opentelemetry.metrics.LongCounter
import io.opentelemetry.metrics.LongSumObserver
import io.opentelemetry.metrics.LongUpDownCounter
import io.opentelemetry.metrics.LongUpDownSumObserver
import io.opentelemetry.metrics.LongValueObserver
import io.opentelemetry.metrics.LongValueRecorder

import javax.management.ObjectName

class OtelHelper {
    private static final String SCALAR = '1'

    private final JmxClient jmxClient
    private final GroovyMetricEnvironment groovyMetricEnvironment

    OtelHelper(JmxClient jmxClient, GroovyMetricEnvironment groovyMetricEnvironment) {
        this.jmxClient = jmxClient
        this.groovyMetricEnvironment = groovyMetricEnvironment
    }

    /**
     * Returns a list of {@link GroovyMBean} for a given object name String.
     * @param objNameStr - the {@link String} representation of an object name or pattern, to be
     * used as the argument to the basic {@link javax.management.ObjectName} constructor for the JmxClient query.
     * @return a {@link List<GroovyMBean>} from which to create metrics.
     */
    List<GroovyMBean> queryJmx(String objNameStr) {
        return MBeanHelper.queryJmx(jmxClient, objNameStr);
    }

    /**
     * Returns a list of {@link GroovyMBean} for a given {@link javax.management.ObjectName}.
     * @param objName - the {@link javax.management.ObjectName} used for the JmxClient query.
     * @return a {@link List<GroovyMBean>} from which to create metrics.
     */
    List<GroovyMBean> queryJmx(ObjectName objName) {
        return MBeanHelper.queryJmx(jmxClient, objName);
    }

    /**
     * Returns a fetched, potentially multi-{@link GroovyMBean} {@link MBeanHelper} for a given object name String.
     * @param objNameStr - the {@link String} representation of an object name or pattern, to be
     * used as the argument to the basic {@link javax.management.ObjectName} constructor for the JmxClient query.
     * @return a {@link MBeanHelper} that operates over all resulting {@link GroovyMBean} instances.
     */
    MBeanHelper mbeans(String objNameStr) {
        def mbeanHelper = new MBeanHelper(jmxClient, objNameStr, false)
        mbeanHelper.fetch()
        return mbeanHelper
    }

    /**
     * Returns a fetched, single {@link GroovyMBean} {@link MBeanHelper} for a given object name String.
     * @param objNameStr - the {@link String} representation of an object name or pattern, to be
     * used as the argument to the basic {@link javax.management.ObjectName} constructor for the JmxClient query.
     * @return a {@link MBeanHelper} that operates over all resulting {@link GroovyMBean} instances.
     */
    MBeanHelper mbean(String objNameStr) {
        def mbeanHelper = new MBeanHelper(jmxClient, objNameStr, true)
        mbeanHelper.fetch()
        return mbeanHelper
    }

    /**
     * Returns an updated @{link InstrumentHelper} associated with the provided {@link MBeanHelper} and its specified
     * attribute value(s).  The parameters map to the InstrumentHelper constructor.
     */
    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure> labelFuncs, String attribute, Closure otelInstrument) {
        def instrumentHelper = new InstrumentHelper(mBeanHelper, instrumentName, description, unit, labelFuncs, attribute, otelInstrument)
        instrumentHelper.update()
        return instrumentHelper
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, unit, [:] as Map<String, Closure>, attribute, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, OtelHelper.SCALAR, [:] as Map<String, Closure>, attribute, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, "", OtelHelper.SCALAR, [:] as Map<String, Closure>, attribute, otelInstrument)
    }

    DoubleCounter doubleCounter(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getDoubleCounter(name, description, unit, labels)
    }

    DoubleCounter doubleCounter(String name, String description, String unit) {
        return doubleCounter(name, description, unit, null)
    }

    DoubleCounter doubleCounter(String name, String description) {
        return doubleCounter(name, description, SCALAR)
    }

    DoubleCounter doubleCounter(String name) {
        return doubleCounter(name, '')
    }

    LongCounter longCounter(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getLongCounter(name, description, unit, labels)
    }

    LongCounter longCounter(String name, String description, String unit) {
        return longCounter(name, description, unit, null)
    }

    LongCounter longCounter(String name, String description) {
        return longCounter(name, description, SCALAR)
    }

    LongCounter longCounter(String name) {
        return longCounter(name, '')
    }

    DoubleUpDownCounter doubleUpDownCounter(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getDoubleUpDownCounter(name, description, unit, labels)
    }

    DoubleUpDownCounter doubleUpDownCounter(String name, String description, String unit) {
        return doubleUpDownCounter(name, description, unit, null)
    }

    DoubleUpDownCounter doubleUpDownCounter(String name, String description) {
        return doubleUpDownCounter(name, description, SCALAR)
    }

    DoubleUpDownCounter doubleUpDownCounter(String name) {
        return doubleUpDownCounter(name, '')
    }

    LongUpDownCounter longUpDownCounter(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getLongUpDownCounter(name, description, unit, labels)
    }

    LongUpDownCounter longUpDownCounter(String name, String description, String unit) {
        return longUpDownCounter(name, description, unit, null)
    }

    LongUpDownCounter longUpDownCounter(String name, String description) {
        return longUpDownCounter(name, description, SCALAR)
    }

    LongUpDownCounter longUpDownCounter(String name) {
        return longUpDownCounter(name, '')
    }

    DoubleValueRecorder doubleValueRecorder(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getDoubleValueRecorder(name, description, unit, labels)
    }

    DoubleValueRecorder doubleValueRecorder(String name, String description, String unit) {
        return doubleValueRecorder(name, description, unit, null)
    }

    DoubleValueRecorder doubleValueRecorder(String name, String description) {
        return doubleValueRecorder(name, description, SCALAR)
    }

    DoubleValueRecorder doubleValueRecorder(String name) {
        return doubleValueRecorder(name, '')
    }

    LongValueRecorder longValueRecorder(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getLongValueRecorder(name, description, unit, labels)
    }

    LongValueRecorder longValueRecorder(String name, String description, String unit) {
        return longValueRecorder(name, description, unit, null)
    }

    LongValueRecorder longValueRecorder(String name, String description) {
        return longValueRecorder(name, description, SCALAR)
    }

    LongValueRecorder longValueRecorder(String name) {
        return longValueRecorder(name, '')
    }

    DoubleSumObserver doubleSumObserver(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getDoubleSumObserver(name, description, unit, labels)
    }

    DoubleSumObserver doubleSumObserver(String name, String description, String unit) {
        return doubleSumObserver(name, description, unit, null)
    }

    DoubleSumObserver doubleSumObserver(String name, String description) {
        return doubleSumObserver(name, description, SCALAR)
    }

    DoubleSumObserver doubleSumObserver(String name) {
        return doubleSumObserver(name, '')
    }

    LongSumObserver longSumObserver(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getLongSumObserver(name, description, unit, labels)
    }

    LongSumObserver longSumObserver(String name, String description, String unit) {
        return longSumObserver(name, description, unit, null)
    }

    LongSumObserver longSumObserver(String name, String description) {
        return longSumObserver(name, description, SCALAR)
    }

    LongSumObserver longSumObserver(String name) {
        return longSumObserver(name, '')
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getDoubleUpDownSumObserver(name, description, unit, labels)
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name, String description, String unit) {
        return doubleUpDownSumObserver(name, description, unit, null)
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name, String description) {
        return doubleUpDownSumObserver(name, description, SCALAR)
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name) {
        return doubleUpDownSumObserver(name, '')
    }

    LongUpDownSumObserver longUpDownSumObserver(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getLongUpDownSumObserver(name, description, unit, labels)
    }

    LongUpDownSumObserver longUpDownSumObserver(String name, String description, String unit) {
        return longUpDownSumObserver(name, description, unit, null)
    }

    LongUpDownSumObserver longUpDownSumObserver(String name, String description) {
        return longUpDownSumObserver(name, description, SCALAR)
    }

    LongUpDownSumObserver longUpDownSumObserver(String name) {
        return longUpDownSumObserver(name, '')
    }

    DoubleValueObserver doubleValueObserver(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getDoubleValueObserver(name, description, unit, labels)
    }

    DoubleValueObserver doubleValueObserver(String name, String description, String unit) {
        return doubleValueObserver(name, description, unit, null)
    }

    DoubleValueObserver doubleValueObserver(String name, String description) {
        return doubleValueObserver(name, description, SCALAR)
    }

    DoubleValueObserver doubleValueObserver(String name) {
        return doubleValueObserver(name, '')
    }

    LongValueObserver longValueObserver(String name, String description, String unit, Map<String, String> labels) {
        return groovyMetricEnvironment.getLongValueObserver(name, description, unit, labels)
    }

    LongValueObserver longValueObserver(String name, String description, String unit) {
        return longValueObserver(name, description, unit, null)
    }

    LongValueObserver longValueObserver(String name, String description) {
        return longValueObserver(name, description, SCALAR)
    }

    LongValueObserver longValueObserver(String name) {
        return longValueObserver(name, '')
    }
}
