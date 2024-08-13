/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import groovy.jmx.GroovyMBean
import groovy.transform.PackageScope
import io.opentelemetry.api.metrics.ObservableMeasurement

import javax.management.AttributeNotFoundException
import javax.management.InvalidAttributeValueException
import java.util.logging.Logger
import javax.management.openmbean.CompositeData

/**
 * A helper for easy instrument creation and updates based on an
 * {@link MBeanHelper} attribute's value and passed {@link OtelHelper}
 * instrument creator method pointer (e.g. &longCounter).
 *
 * Intended to be used via the script-bound `otel` {@link OtelHelper} instance methods:
 *
 * otel.instrument(myThreadingMBeanHelper,
 *       "jvm.threads.count", "number of threads",
 *       "1", [
 *         "myLabel": { mbean -> mbean.name().getKeyProperty("myObjectNameProperty") },
 *         "myOtherLabel": { "myLabelValue" }
 *       ], "ThreadCount", otel.&longUpDownCounter)
 *
 *
 * If the underlying MBean(s) held by the MBeanHelper are
 * {@link CompositeData} instances, each key of their CompositeType's
 * keySet will be .-appended to the specified instrumentName and
 * updated for each respective value.
 */
class InstrumentHelper {
    private static final Logger logger = Logger.getLogger(InstrumentHelper.class.getName())

    private final MBeanHelper mBeanHelper
    private final String instrumentName
    private final String description
    private final String unit
    private final Map<String, Map<String, Closure>> mBeanAttributes
    private final Map<String, Closure> labelFuncs
    private final Closure instrument
    private final GroovyMetricEnvironment metricEnvironment
    private final boolean aggregateAcrossMBeans

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
     * {@link GroovyMBean}-provided Closures: (e.g. [ "myLabelName" : { mbean -> "myLabelValue"} ]). The
     *        resulting Label instances will be used for each individual update.
     * @param attribute - The {@link GroovyMBean} attribute for which to use as the instrument value.
     * @param instrument - The {@link io.opentelemetry.api.metrics.Instrument}-producing {@link OtelHelper} method pointer:
     *        (e.g. new OtelHelper().&doubleValueRecorder)
     * @param metricenvironment - The {@link GroovyMetricEnvironment} used to register callbacks onto the SDK meter for
     *        batch callbacks used to handle {@link CompositeData}
     * @param aggregateAcrossMBeans - Whether to aggregate multiple MBeans together before recording.
     */
    InstrumentHelper(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure<?>> labelFuncs, Map<String, Map<String, Closure<?>>> MBeanAttributes, Closure<?> instrument, GroovyMetricEnvironment metricEnvironment, boolean aggregateAcrossMBeans) {
        this.mBeanHelper = mBeanHelper
        this.instrumentName = instrumentName
        this.description = description
        this.unit = unit
        this.labelFuncs = labelFuncs
        this.mBeanAttributes = MBeanAttributes
        this.instrument = instrument
        this.metricEnvironment = metricEnvironment
        this.aggregateAcrossMBeans = aggregateAcrossMBeans
    }

    void update() {
        def mbeans = mBeanHelper.getMBeans()
        def compositeAttributes = []
        def simpleAttributes = []
        if (mbeans.size() == 0) {
            return
        }

        mBeanAttributes.keySet().each { attribute ->
            try {
                // Look at the collected mbeans to evaluate if the attributes requested are
                // composite data types or simple. Composite types require different parsing to
                // end up with multiple recorders in the same callback.
                def keySet = getCompositeKeys(attribute, mbeans)
                if (keySet.size() > 0) {
                    compositeAttributes.add(new Tuple2<String, Set<String>>(attribute, keySet))
                } else {
                    simpleAttributes.add(attribute)
                }
            } catch (AttributeNotFoundException ignored) {
                logger.fine("Attribute ${attribute} not found on any of the collected mbeans")
            } catch (InvalidAttributeValueException ignored) {
                logger.info("Attribute ${attribute} was not consistently CompositeData for " +
                  "collected mbeans. The metrics gatherer cannot collect measurements for an instrument " +
                  "when the mbeans attribute values are not all CompositeData or all simple values.")
            }
        }

        if (simpleAttributes.size() > 0) {
            def simpleUpdateClosure = prepareUpdateClosure(mbeans, simpleAttributes)
            if (instrumentIsDoubleObserver(instrument) || instrumentIsLongObserver(instrument)) {
                instrument(instrumentName, description, unit, { result ->
                    simpleUpdateClosure(result)
                })
            } else {
                simpleUpdateClosure(instrument(instrumentName, description, unit))
            }
        }

        if (compositeAttributes.size() > 0) {
            registerCompositeUpdateClosures(mbeans, compositeAttributes)
        }
    }

    // This function retrieves the set of CompositeData keys for the given attribute for the currently
    // collected mbeans. If the attribute is all simple values it will return an empty list.
    // If the attribute is inconsistent across mbeans, it will throw an exception.
    private static Set<String> getCompositeKeys(String attribute, List<GroovyMBean> beans) throws AttributeNotFoundException, InvalidAttributeValueException {
        def isComposite = false
        def isFound = false
        def keySet = beans.collect { bean ->
            try {
                def value = MBeanHelper.getBeanAttribute(bean, attribute)
                if (value == null) {
                    // Null represents an attribute not found exception in MBeanHelper
                    []
                } else if (value instanceof CompositeData) {
                    // If we've found a simple attribute, throw an exception as this attribute
                    // was mixed between simple & composite
                    if (!isComposite && isFound) {
                        throw new InvalidAttributeValueException()
                    }
                    isComposite = true
                    isFound = true
                    value.getCompositeType().keySet()
                } else {
                    // If we've previously found a composite attribute, throw an exception as this attribute
                    // was mixed between simple & composite
                    if (isComposite) {
                        throw new InvalidAttributeValueException()
                    }
                    isFound = true
                    []
                }
            } catch (AttributeNotFoundException | NullPointerException ignored) {
                []
            }
        }.flatten()
          .toSet()

        if (!isFound) {
            throw new AttributeNotFoundException()
        }

        return keySet
    }

    private static Map<String, String> getLabels(GroovyMBean mbean, Map<String, Closure> labelFuncs, Map<String, Closure> additionalLabels) {
        def labels = [:]
        labelFuncs.each { label, labelFunc ->
            try {
                labels[label] = labelFunc(mbean) as String
            } catch(AttributeNotFoundException e) {
                logger.warning("Attribute missing for label:${label}, label was not applied")
            }
        }
        additionalLabels.each { label, labelFunc ->
            try {
                labels[label] = labelFunc(mbean) as String
            } catch(AttributeNotFoundException e) {
                logger.warning("Attribute missing for label:${label}, label was not applied")
            }
        }
        return labels
    }

    private static String getAggregationKey(String instrumentName, Map<String, String> labels) {
        def labelsKey = labels.sort().collect { key, value -> "${key}:${value}" }.join(";")
        return "${instrumentName}/${labelsKey}"
    }

    // Create a closure for simple attributes that will retrieve mbean information on
    // callback to ensure that metrics are collected on request
    private Closure prepareUpdateClosure(List<GroovyMBean> mbeans, attributes) {
        return { result ->
            def aggregations = [:] as Map<String, Aggregation>
            boolean requireAggregation = aggregateAcrossMBeans && mbeans.size() > 1 && instrumentIsValue(instrument)
            [mbeans, attributes].combinations().each { pair ->
                def (mbean, attribute) = pair
                def value = MBeanHelper.getBeanAttribute(mbean, attribute)
                if (value != null) {
                    def labels = getLabels(mbean, labelFuncs, mBeanAttributes[attribute])
                    if (requireAggregation) {
                        def key = getAggregationKey(instrumentName, labels)
                        if (aggregations[key] == null) {
                            aggregations[key] = new Aggregation(labels)
                        }
                        logger.fine("Aggregating ${mbean.name()} ${instrumentName} - ${instrument.method} w/ ${value} - ${labels}")
                        aggregations[key].add(value)
                    } else {
                        logger.fine("Recording ${mbean.name()} ${instrumentName} - ${instrument.method} w/ ${value} - ${labels}")
                        recordDataPoint(instrument, result, value, GroovyMetricEnvironment.mapToAttributes(labels))
                    }
                }
            }
            aggregations.each { entry ->
                logger.fine("Recording ${instrumentName} - ${instrument.method} - w/ ${entry.value.value} - ${entry.value.labels}")
                recordDataPoint(instrument, result, entry.value.value, GroovyMetricEnvironment.mapToAttributes(entry.value.labels))
            }
        }
    }

    // Create a closure for composite data attributes that will retrieve mbean information
    // on callback to ensure that metrics are collected on request. This will create a single
    // batch callback for all of the metrics collected on a single attribute.
    private void registerCompositeUpdateClosures(List<GroovyMBean> mbeans, attributes) {
        attributes.each { pair ->
            def (attribute, keys) = pair
            def instruments = keys.collect { new Tuple2<String, ObservableMeasurement>(it, instrument("${instrumentName}.${it}", description, unit, null)) }

            metricEnvironment.registerBatchCallback("${instrumentName}.${attribute}", () -> {
                mbeans.each { mbean ->
                    def value = MBeanHelper.getBeanAttribute(mbean, attribute)
                    if (value != null && value instanceof CompositeData) {
                        instruments.each { inst ->
                            def val = value.get(inst.v1)
                            def labels = getLabels(mbean, labelFuncs, mBeanAttributes[attribute])
                            logger.fine("Recording ${"${instrumentName}.${inst.v1}"} - ${instrument.method} w/ ${val} - ${labels}")
                            recordDataPoint(instrument, inst.v2, val, GroovyMetricEnvironment.mapToAttributes(labels))
                        }
                    }
                }
            }, instruments.first().v2, *instruments.tail().collect { it.v2 })
        }
    }

    // Based on the type of instrument, record the data point in the way expected by the observable
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

    @PackageScope
    static boolean instrumentIsDoubleObserver(inst) {
        return [
          "doubleCounterCallback",
          "doubleUpDownCounterCallback",
          "doubleValueCallback",
        ].contains(inst.method)
    }

    @PackageScope
    static boolean instrumentIsLongObserver(inst) {
        return [
          "longCounterCallback",
          "longUpDownCounterCallback",
          "longValueCallback",
        ].contains(inst.method)
    }

    @PackageScope
    static boolean instrumentIsValue(inst) {
        return [
            "doubleValueCallback",
            "longValueCallback"
        ].contains(inst.method)
    }

    @PackageScope
    static boolean instrumentIsCounter(inst) {
        return [
          "doubleCounter",
          "doubleUpDownCounter",
          "longCounter",
          "longUpDownCounter"
        ].contains(inst.method)
    }

    static class Aggregation {
        private final Map<String, String> labels
        private def value

        Aggregation(Map<String, String> labels) {
            this.labels = labels
            this.value = 0.0
        }

        void add(value) {
            this.value += value
        }
    }
}
