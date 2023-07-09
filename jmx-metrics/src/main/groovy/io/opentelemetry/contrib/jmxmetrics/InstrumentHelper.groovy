/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import groovy.jmx.GroovyMBean
import groovy.transform.PackageScope

import javax.management.AttributeNotFoundException
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
    private final Map<String, Map<String, Closure>> mBeanAttributes
    private final Map<String, Closure> labelFuncs
    private final Closure instrument

    /**
     * An InstrumentHelper provides the ability to easily create and update {@link io.opentelemetry.api.metrics.Instrument}
     * instances from an MBeanHelper's underlying {@link GroovyMBean} instances via an {@link OtelHelper}'s instrument
     * method pointer.
     *
     * @param mBeanHelper - the single or multiple {@link GroovyMBean}-representing MBeanHelper from which to access attribute values
     * @param instrumentName - the resulting instruments' name to register.
     * @param description - the resulting instruments' description to register.
     * @param unit - the resulting instruments' unit to register.
     * @param labelFuncs - A {@link Map<String, Closure>} of label names and values to be determined by custom
     *        {@link GroovyMBean}-provided Closures: (e.g. [ "myLabelName" : { mbean -> "myLabelValue"} ]). The
     *        resulting Label instances will be used for each individual update.
     * @param attribute - The {@link GroovyMBean} attribute for which to use as the instrument value.
     * @param instrument - The {@link io.opentelemetry.api.metrics.Instrument}-producing {@link OtelHelper} method pointer:
     *        (e.g. new OtelHelper().&doubleValueRecorder)
     */
    InstrumentHelper(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure<?>> labelFuncs, Map<String, Map<String, Closure<?>>> MBeanAttributes, Closure<?> instrument) {
        this.mBeanHelper = mBeanHelper
        this.instrumentName = instrumentName
        this.description = description
        this.unit = unit
        this.labelFuncs = labelFuncs
        this.mBeanAttributes = MBeanAttributes
        this.instrument = instrument
    }

    void update() {
        def updateClosure = prepareUpdateClosure(instrument, mBeanHelper.getMBeans(), mBeanAttributes.keySet())
        if (instrumentIsDoubleObserver(instrument) || instrumentIsLongObserver(instrument)) {
          instrument(instrumentName, description, unit, { result ->
            updateClosure(result)
          })
        } else {
          updateClosure(instrument(instrumentName, description, unit))
        }
    }

    private static Map<String, String> getLabels(GroovyMBean mbean, Map<String, Closure> labelFuncs, Map<String, Closure> additionalLabels) {
        def labels = [:]
        labelFuncs.each { label, labelFunc ->
          labels[label] = labelFunc(mbean) as String
        }
        additionalLabels.each {label, labelFunc ->
            labels[label] = labelFunc(mbean) as String
        }
        return labels
    }

  private Closure prepareUpdateClosure(inst, List<GroovyMBean> mbeans, attributes) {
    return { result ->
      [mbeans, attributes].combinations().each { pair ->
        def (mbean, attribute) = pair
        try {
          def value = mbean.getProperty(attribute)
          if (value instanceof CompositeData) {
            value.getCompositeType().keySet().each { key ->
              def val = value.get(key)
              def updatedInstrumentName = "${instrumentName}.${key}"
              def labels = getLabels(mbean, labelFuncs, mBeanAttributes[attribute])
              logger.fine("Recording ${updatedInstrumentName} - ${instrument.method} w/ ${val} - ${labels}")
              recordDataPoint(inst, result, val, GroovyMetricEnvironment.mapToAttributes(labels))
            }
          } else if (value != null) {
            def labels = getLabels(mbean, labelFuncs, mBeanAttributes[attribute])
            logger.fine("Recording ${instrumentName} - ${instrument.method} w/ ${value} - ${labels}")
            recordDataPoint(inst, result, value, GroovyMetricEnvironment.mapToAttributes(labels))
          }
        } catch (AttributeNotFoundException e ) {
          logger.info("Expected attribute ${attribute} not found in mbean ${mbean.name()}")
        }
      }
    }
  }

  private static void recordDataPoint(inst, result, value, labelMap) {
    if (instrumentIsLongObserver(inst)) {
        result.record((long) value, labelMap)
    } else if (instrumentIsDoubleObserver(inst)) {
        result.record((double) value, labelMap)
    } else if (instrumentIsCounter(inst)) {
        result.add(value, labelMap)
    } else {
        result.record(value, labelMap)
    }
  }

    @PackageScope static boolean instrumentIsDoubleObserver(inst) {
        return [
            "doubleCounterCallback",
            "doubleUpDownCounterCallback",
            "doubleValueCallback",
        ].contains(inst.method)
    }

    @PackageScope static boolean instrumentIsLongObserver(inst) {
        return [
            "longCounterCallback",
            "longUpDownCounterCallback",
            "longValueCallback",
        ].contains(inst.method)
    }

    @PackageScope static boolean instrumentIsCounter(inst) {
        return [
            "doubleCounter",
            "doubleUpDownCounter",
            "longCounter",
            "longUpDownCounter"
        ].contains(inst.method)
    }
}
