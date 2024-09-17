/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import groovy.jmx.GroovyMBean
import io.opentelemetry.api.metrics.DoubleCounter
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.DoubleUpDownCounter
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import io.opentelemetry.api.metrics.LongUpDownCounter
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import java.util.function.Consumer
import javax.management.ObjectName

class OtelHelper {
    private static final String SCALAR = '1'

    private final JmxClient jmxClient
    private final GroovyMetricEnvironment groovyMetricEnvironment
    private final boolean aggregateAcrossMBeans

    OtelHelper(JmxClient jmxClient, GroovyMetricEnvironment groovyMetricEnvironment, boolean aggregateAcrossMBeans) {
        this.jmxClient = jmxClient
        this.groovyMetricEnvironment = groovyMetricEnvironment
        this.aggregateAcrossMBeans = aggregateAcrossMBeans
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

    MBeanHelper mbean(String objNameStr, Map<String,Closure<?>> attributeTransformation) {
      def mbeanHelper = new MBeanHelper(jmxClient, objNameStr, true, attributeTransformation)
      mbeanHelper.fetch()
      return mbeanHelper
    }

    MBeanHelper mbeans(List<String> objNameStrs, Map<String,Closure<?>> attributeTransformation) {
      def mbeanHelper = new MBeanHelper(jmxClient, objNameStrs, attributeTransformation)
      mbeanHelper.fetch()
      return mbeanHelper
    }
    /**
     * Returns an updated @{link InstrumentHelper} associated with the provided {@link MBeanHelper} and its specified
     * attribute value(s).  The parameters map to the InstrumentHelper constructor.
     */
    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure> labelFuncs, Map<String, Map<String, Closure>> attributes, Closure otelInstrument) {
        def instrumentHelper = new InstrumentHelper(mBeanHelper, instrumentName, description, unit, labelFuncs, attributes, otelInstrument, groovyMetricEnvironment, aggregateAcrossMBeans)
        instrumentHelper.update()
        return instrumentHelper
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure> labelFuncs, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, unit, labelFuncs, [(attribute): [:] as Map<String, Closure>], otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, unit, [:] as Map<String, Closure>, attribute, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Map<String, Closure>> attributes, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, unit, [:] as Map<String, Closure>, attributes, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, OtelHelper.SCALAR, [:] as Map<String, Closure>, attribute, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String description, Map<String, Map<String, Closure>> attributes, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, description, OtelHelper.SCALAR, [:] as Map<String, Closure>, attributes, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, String attribute, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, "", OtelHelper.SCALAR, [:] as Map<String, Closure>, attribute, otelInstrument)
    }

    InstrumentHelper instrument(MBeanHelper mBeanHelper, String instrumentName, Map<String, Map<String, Closure>> attributes, Closure otelInstrument) {
        return instrument(mBeanHelper, instrumentName, "", OtelHelper.SCALAR, [:] as Map<String, Closure>, attributes, otelInstrument)
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

    DoubleHistogram doubleHistogram(String name, String description, String unit) {
        return groovyMetricEnvironment.getDoubleHistogram(name, description, unit)
    }

    DoubleHistogram doubleHistogram(String name, String description) {
        return doubleHistogram(name, description, SCALAR)
    }

    DoubleHistogram doubleHistogram(String name) {
        return doubleHistogram(name, '')
    }

    LongHistogram longHistogram(String name, String description, String unit) {
        return groovyMetricEnvironment.getLongHistogram(name, description, unit)
    }

    LongHistogram longHistogram(String name, String description) {
        return longHistogram(name, description, SCALAR)
    }

    LongHistogram longHistogram(String name) {
        return longHistogram(name, '')
    }

    ObservableDoubleMeasurement doubleCounterCallback(String name, String description, String unit, Consumer<ObservableDoubleMeasurement> updater) {
        return groovyMetricEnvironment.registerDoubleCounterCallback(name, description, unit, updater)
    }

    ObservableDoubleMeasurement doubleCounterCallback(String name, String description, Consumer<ObservableDoubleMeasurement> updater) {
        return doubleCounterCallback(name, description, SCALAR, updater)
    }

    ObservableDoubleMeasurement doubleCounterCallback(String name, Consumer<ObservableDoubleMeasurement> updater) {
        return doubleCounterCallback(name, '', updater)
    }

    ObservableLongMeasurement longCounterCallback(String name, String description, String unit, Consumer<ObservableLongMeasurement> updater) {
        return groovyMetricEnvironment.registerLongCounterCallback(name, description, unit, updater)
    }

    ObservableLongMeasurement longCounterCallback(String name, String description, Consumer<ObservableLongMeasurement> updater) {
        return longCounterCallback(name, description, SCALAR, updater)
    }

    ObservableLongMeasurement longCounterCallback(String name, Consumer<ObservableLongMeasurement> updater) {
        return longCounterCallback(name, '', updater)
    }

    ObservableDoubleMeasurement doubleUpDownCounterCallback(String name, String description, String unit, Consumer<ObservableDoubleMeasurement> updater) {
        return groovyMetricEnvironment.registerDoubleUpDownCounterCallback(name, description, unit, updater)
    }

    ObservableDoubleMeasurement doubleUpDownCounterCallback(String name, String description, Consumer<ObservableDoubleMeasurement> updater) {
        return doubleUpDownCounterCallback(name, description, SCALAR, updater)
    }

    ObservableDoubleMeasurement doubleUpDownCounterCallback(String name, Consumer<ObservableDoubleMeasurement> updater) {
        return doubleUpDownCounterCallback(name, '', updater)
    }

    ObservableLongMeasurement longUpDownCounterCallback(String name, String description, String unit, Consumer<ObservableLongMeasurement> updater) {
        return groovyMetricEnvironment.registerLongUpDownCounterCallback(name, description, unit, updater)
    }

    ObservableLongMeasurement longUpDownCounterCallback(String name, String description, Consumer<ObservableLongMeasurement> updater) {
        return longUpDownCounterCallback(name, description, SCALAR, updater)
    }

    ObservableLongMeasurement longUpDownCounterCallback(String name, Consumer<ObservableLongMeasurement> updater) {
        return longUpDownCounterCallback(name, '', updater)
    }

    ObservableDoubleMeasurement doubleValueCallback(String name, String description, String unit, Consumer<ObservableDoubleMeasurement> updater) {
        return groovyMetricEnvironment.registerDoubleValueCallback(name, description, unit, updater)
    }

    ObservableDoubleMeasurement doubleValueCallback(String name, String description, Consumer<ObservableDoubleMeasurement> updater) {
        return doubleValueCallback(name, description, SCALAR, updater)
    }

    ObservableDoubleMeasurement doubleValueCallback(String name, Consumer<ObservableDoubleMeasurement> updater) {
        return doubleValueCallback(name, '', updater)
    }

    ObservableLongMeasurement longValueCallback(String name, String description, String unit, Consumer<ObservableLongMeasurement> updater) {
        return groovyMetricEnvironment.registerLongValueCallback(name, description, unit, updater)
    }

    ObservableLongMeasurement longValueCallback(String name, String description, Consumer<ObservableLongMeasurement> updater) {
        return longValueCallback(name, description, SCALAR, updater)
    }

    ObservableLongMeasurement longValueCallback(String name, Consumer<ObservableLongMeasurement> updater) {
        return longValueCallback(name, '', updater)
    }
}
