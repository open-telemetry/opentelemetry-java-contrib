/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import io.opentelemetry.api.metrics.DoubleCounter
import io.opentelemetry.api.metrics.DoubleSumObserver
import io.opentelemetry.api.metrics.DoubleUpDownCounter
import io.opentelemetry.api.metrics.DoubleUpDownSumObserver
import io.opentelemetry.api.metrics.DoubleValueObserver
import io.opentelemetry.api.metrics.DoubleValueRecorder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongSumObserver
import io.opentelemetry.api.metrics.LongUpDownCounter
import io.opentelemetry.api.metrics.LongUpDownSumObserver
import io.opentelemetry.api.metrics.LongValueObserver
import io.opentelemetry.api.metrics.LongValueRecorder

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
     * Returns a fetched, potentially multi-{@link GroovyMBean} {@link MBeanHelper} for a given object name String.
     * @param objNameStr - the {@link String} representation of an object name or pattern, to be
     * used as the argument to the basic {@link javax.management.ObjectName} constructor for the JmxClient query.
     * @return a {@link MBeanHelper} that operates over all resulting {@link GroovyMBean} instances.
     */
    MBeanHelper mbeans(List<String> objNameStrs) {
        def mbeanHelper = new MBeanHelper(jmxClient, objNameStrs)
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

    DoubleCounter doubleCounter(String name, String description, String unit) {
        return groovyMetricEnvironment.getDoubleCounter(name, description, unit)
    }

    DoubleCounter doubleCounter(String name, String description) {
        return doubleCounter(name, description, SCALAR)
    }

    DoubleCounter doubleCounter(String name) {
        return doubleCounter(name, '')
    }

    LongCounter longCounter(String name, String description, String unit) {
        return groovyMetricEnvironment.getLongCounter(name, description, unit)
    }

    LongCounter longCounter(String name, String description) {
        return longCounter(name, description, SCALAR)
    }

    LongCounter longCounter(String name) {
        return longCounter(name, '')
    }

    DoubleUpDownCounter doubleUpDownCounter(String name, String description, String unit) {
        return groovyMetricEnvironment.getDoubleUpDownCounter(name, description, unit)
    }

    DoubleUpDownCounter doubleUpDownCounter(String name, String description) {
        return doubleUpDownCounter(name, description, SCALAR)
    }

    DoubleUpDownCounter doubleUpDownCounter(String name) {
        return doubleUpDownCounter(name, '')
    }

    LongUpDownCounter longUpDownCounter(String name, String description, String unit) {
        return groovyMetricEnvironment.getLongUpDownCounter(name, description, unit)
    }

    LongUpDownCounter longUpDownCounter(String name, String description) {
        return longUpDownCounter(name, description, SCALAR)
    }

    LongUpDownCounter longUpDownCounter(String name) {
        return longUpDownCounter(name, '')
    }

    DoubleValueRecorder doubleValueRecorder(String name, String description, String unit) {
        return groovyMetricEnvironment.getDoubleValueRecorder(name, description, unit)
    }

    DoubleValueRecorder doubleValueRecorder(String name, String description) {
        return doubleValueRecorder(name, description, SCALAR)
    }

    DoubleValueRecorder doubleValueRecorder(String name) {
        return doubleValueRecorder(name, '')
    }

    LongValueRecorder longValueRecorder(String name, String description, String unit) {
        return groovyMetricEnvironment.getLongValueRecorder(name, description, unit)
    }

    LongValueRecorder longValueRecorder(String name, String description) {
        return longValueRecorder(name, description, SCALAR)
    }

    LongValueRecorder longValueRecorder(String name) {
        return longValueRecorder(name, '')
    }

    DoubleSumObserver doubleSumObserver(String name, String description, String unit, Closure updater) {
        return groovyMetricEnvironment.getDoubleSumObserver(name, description, unit, updater)
    }

    DoubleSumObserver doubleSumObserver(String name, String description, Closure updater) {
        return doubleSumObserver(name, description, SCALAR, updater)
    }

    DoubleSumObserver doubleSumObserver(String name, Closure updater) {
        return doubleSumObserver(name, '', updater)
    }

    LongSumObserver longSumObserver(String name, String description, String unit, Closure updater) {
        return groovyMetricEnvironment.getLongSumObserver(name, description, unit, updater)
    }

    LongSumObserver longSumObserver(String name, String description, Closure updater) {
        return longSumObserver(name, description, SCALAR, updater)
    }

    LongSumObserver longSumObserver(String name, Closure updater) {
        return longSumObserver(name, '', updater)
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name, String description, String unit, Closure updater) {
        return groovyMetricEnvironment.getDoubleUpDownSumObserver(name, description, unit, updater)
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name, String description, Closure updater) {
        return doubleUpDownSumObserver(name, description, SCALAR, updater)
    }

    DoubleUpDownSumObserver doubleUpDownSumObserver(String name, Closure updater) {
        return doubleUpDownSumObserver(name, '', updater)
    }

    LongUpDownSumObserver longUpDownSumObserver(String name, String description, String unit, Closure updater) {
        return groovyMetricEnvironment.getLongUpDownSumObserver(name, description, unit, updater)
    }

    LongUpDownSumObserver longUpDownSumObserver(String name, String description, Closure updater) {
        return longUpDownSumObserver(name, description, SCALAR, updater)
    }

    LongUpDownSumObserver longUpDownSumObserver(String name, Closure updater) {
        return longUpDownSumObserver(name, '', updater)
    }

    DoubleValueObserver doubleValueObserver(String name, String description, String unit, Closure updater) {
        return groovyMetricEnvironment.getDoubleValueObserver(name, description, unit, updater)
    }

    DoubleValueObserver doubleValueObserver(String name, String description, Closure updater) {
        return doubleValueObserver(name, description, SCALAR, updater)
    }

    DoubleValueObserver doubleValueObserver(String name, Closure updater) {
        return doubleValueObserver(name, '', updater)
    }

    LongValueObserver longValueObserver(String name, String description, String unit, Closure updater) {
        return groovyMetricEnvironment.getLongValueObserver(name, description, unit, updater)
    }

    LongValueObserver longValueObserver(String name, String description, Closure updater) {
        return longValueObserver(name, description, SCALAR, updater)
    }

    LongValueObserver longValueObserver(String name, Closure updater) {
        return longValueObserver(name, '', updater)
    }
}
