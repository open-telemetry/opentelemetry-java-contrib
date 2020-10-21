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
import io.opentelemetry.metrics.DoubleUpDownCounter
import io.opentelemetry.metrics.LongCounter
import io.opentelemetry.metrics.LongUpDownCounter
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

    private MBeanHelper mBeanHelper
    private String instrumentName
    private String description
    private String unit
    private String attribute
    private Map<String, Closure> labelFuncs
    private Closure instrument

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

        [mbeans, values].transpose().each { mbean, value ->
            if (value instanceof CompositeData) {
                value.getCompositeType().keySet().each { key ->
                    def val = value.get(key)
                    def updatedInstrumentName = "${instrumentName}.${key}"

                    def labels = [:]
                    labelFuncs.each { label, labelFunc ->
                        labels[label] = labelFunc(mbean) as String
                    }

                    def inst = instrument(updatedInstrumentName, description, unit)
                    logger.fine("Recording ${updatedInstrumentName} - ${inst} w/ ${val} - ${labels}")
                    updateInstrumentWithValue(inst, val, labels)
                }
            } else {
                def labels = [:]
                labelFuncs.each { label, labelFunc ->
                    labels[label] = labelFunc(mbean) as String
                }
                def inst = instrument(instrumentName, description, unit)

                logger.fine("Recording ${instrumentName} - ${inst} w/ ${value} - ${labels}")
                updateInstrumentWithValue(inst, value, labels)
            }
        }
    }

    private static void updateInstrumentWithValue(inst, value, labels) {
        def labelMap = GroovyMetricEnvironment.mapToLabels(labels)
        if (inst instanceof DoubleCounter
        || inst instanceof DoubleUpDownCounter
        || inst instanceof LongCounter
        || inst instanceof LongUpDownCounter) {
            inst.add(value, labelMap)
        } else {
            inst.record(value, labelMap)
        }
    }
}
