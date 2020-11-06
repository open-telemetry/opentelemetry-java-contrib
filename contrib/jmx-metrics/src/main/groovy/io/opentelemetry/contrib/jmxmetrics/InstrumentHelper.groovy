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
import io.opentelemetry.metrics.LongCounter
import io.opentelemetry.metrics.LongSumObserver
import io.opentelemetry.metrics.LongUpDownCounter
import io.opentelemetry.metrics.LongUpDownSumObserver
import io.opentelemetry.metrics.LongValueObserver

import java.util.logging.Logger
import javax.management.openmbean.CompositeData

/**
 * A helper for easy instrument creation and updates based on an
 * {@link MBeanHelper} attribute's value and passed {@link OtelHelper}
 * instrument creator method pointer (e.g. &longCounter).
 *
 * Intended to be used via the script-bound `otel` {@link OtelHelper} instance methods:
 *
 * def threadCount = otel.instrument(myThreadingMBeanHelper,
 *       "jvm.threads.count", "number of threads",
 *       "1", [
 *         "myLabel": { mbean -> mbean.name().getKeyProperty("myObjectNameProperty") },
 *         "myOtherLabel": { "myLabelValue" }
 *       ], "ThreadCount", otel.&longUpDownCounter)
 *
 * threadCount.update()
 *
 * If the underlying MBean(s) held by the MBeanHelper are
 * {@link CompositeData} instances, each key of their CompositeType's
 * keySet will be .-appended to the specified instrumentName and
 * updated for each respective value.
 */
class InstrumentHelper {
    private static final Logger logger = Logger.getLogger(InstrumentHelper.class.getName());

    private final MBeanHelper mBeanHelper
    private final String instrumentName
    private final String description
    private final String unit
    private final String attribute
    private final Map<String, Closure> labelFuncs
    private final Closure instrument

    /**
     * An InstrumentHelper provides the ability to easily create and update {@link io.opentelemetry.metrics.Instrument}
     * instances from an MBeanHelper's underlying {@link GroovyMBean} instances via an {@link OtelHelper}'s instrument
     * method pointer.
     *
     * @param mBeanHelper - the single or multiple {@link GroovyMBean} instance from which to access attribute values
     * @param instrumentName - the resulting instruments' name to register.
     * @param description - the resulting instruments' description to register.
     * @param unit - the resulting instruments' unit to register.
     * @param labelFuncs - A {@link Map<String, Closure>} of label names and values to be determined by custom
     *        {@link GroovyMBean}-provided Closures: (e.g. [ "myLabelName" : { mbean -> "myLabelValue"} ]). The
     *        resulting Label instances will be used for each individual update.
     * @param attribute - The {@link GroovyMBean} attribute for which to use as the instrument value.
     * @param instrument - The {@link io.opentelemetry.metrics.Instrument}-producing {@link OtelHelper} method pointer:
     *        (e.g. new OtelHelper().&doubleValueRecorder)
     */
    InstrumentHelper(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure> labelFuncs, String attribute, Closure instrument) {
        this.mBeanHelper = mBeanHelper
        this.instrumentName = instrumentName
        this.description = description
        this.unit = unit
        this.labelFuncs = labelFuncs
        this.attribute = attribute
        this.instrument = instrument
    }

    void update() {
        def mbeans = mBeanHelper.getMBeans()
        def values = mBeanHelper.getAttribute(attribute)
        if (values.size() == 0) {
            logger.warning("No valid value(s) for ${instrumentName} - ${mBeanHelper}.${attribute}")
            return
        }

        // Observer instruments need to have a single callback set, so pool all update
        // operations in a list of closures per instrument to be executed after all values
        // are established, potentially as the callback.
        def instToUpdates = [:]

        [mbeans, values].transpose().each { mbean, value ->
            if (value instanceof CompositeData) {
                value.getCompositeType().keySet().each { key ->
                    def val = value.get(key)
                    def updatedInstrumentName = "${instrumentName}.${key}"
                    def labels = getLabels(mbean, labelFuncs)
                    def inst = instrument(updatedInstrumentName, description, unit)
                    println "InstrumentHelper.update (composite) - ${inst}"
                    logger.fine("Recording ${updatedInstrumentName} - ${inst} w/ ${val} - ${labels}")
                    if (!instToUpdates.containsKey(inst)) {
                        instToUpdates[inst] = []
                    }
                    instToUpdates[inst].add(prepareUpdateClosure(inst, val, labels))
                }
            } else {
                def labels = getLabels(mbean, labelFuncs)
                def inst = instrument(instrumentName, description, unit)
                println "InstrumentHelper.update - ${inst}"
                logger.fine("Recording ${instrumentName} - ${inst} w/ ${value} - ${labels}")
                if (!instToUpdates.containsKey(inst)) {
                    instToUpdates[inst] = []
                }
                instToUpdates[inst].add(prepareUpdateClosure(inst, value, labels))
            }
        }

        instToUpdates.each {inst, updateClosures ->
            if (instrumentIsObserver(inst)) {
                inst.setCallback({ result ->
                    updateClosures.each { update ->
                        update(result)
                    }
                })
            } else {
                updateClosures.each {
                    it(inst)
                }
            }
        }
    }

    private static Map<String, String> getLabels(GroovyMBean mbean, Map<String, Closure> labelFuncs) {
        def labels = [:]
        labelFuncs.each { label, labelFunc ->
            labels[label] = labelFunc(mbean) as String
        }
        return labels
    }

    private static Closure prepareUpdateClosure(inst, value, labels) {
        def labelMap = GroovyMetricEnvironment.mapToLabels(labels)
        if (instrumentIsObserver(inst)) {
            return { result ->
                result.observe(value, labelMap)
            }
        } else if (instrumentIsCounter(inst)) {
            return { i -> i.add(value, labelMap) }
        } else {
            return { i -> i.record(value, labelMap) }
        }
    }

    private static boolean instrumentIsObserver(inst) {
        return (inst instanceof DoubleSumObserver
                || inst instanceof DoubleUpDownSumObserver
                || inst instanceof LongSumObserver
                || inst instanceof LongUpDownSumObserver
                || inst instanceof DoubleValueObserver
                || inst instanceof LongValueObserver)
    }

    private static boolean instrumentIsCounter(inst) {
        return (inst instanceof DoubleCounter
                || inst instanceof DoubleUpDownCounter
                || inst instanceof LongCounter
                || inst instanceof LongUpDownCounter)
    }
}
