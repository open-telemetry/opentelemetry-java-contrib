/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import groovy.jmx.GroovyMBean
import groovy.transform.PackageScope
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.AttributeNotFoundException
import java.util.logging.Logger

/**
 * A helper for easy {@link GroovyMBean} querying, creation, and protected attribute access.
 *
 * Basic query functionality is provided by the static queryJmx methods, while MBeanHelper
 * instances operate on an underlying GroovyMBean list field.
 *
 * Specifying if a single underlying GroovyMBean is expected or of interest is done via the
 * isSingle argument, where resulting attribute values are only returned for the first
 * match.
 *
 * Intended to be used via the script-bound `otel` instance:
 *
 * def singleMBean = otel.mbean("com.example:type=SingleType")
 * def multipleMBeans = otel.mbeans("com.example:type=MultipleType,*")
 * [singleMBean, multipleMBeans].each { it.fetch() }
 *
 */
class MBeanHelper {
    private static final Logger logger = Logger.getLogger(MBeanHelper.class.getName());

    private final JmxClient jmxClient
    private final boolean isSingle
    private final List<String> objectNames

    private List<GroovyMBean> mbeans

    MBeanHelper(JmxClient jmxClient, String objectName, boolean isSingle) {
        this.jmxClient = jmxClient
        this.objectNames = Collections.unmodifiableList([objectName])
        this.isSingle = isSingle
    }

    MBeanHelper(JmxClient jmxClient, List<String> objectNames) {
        this.jmxClient = jmxClient
        this.objectNames = Collections.unmodifiableList(objectNames)
        this.isSingle = false
    }

    @PackageScope static List<GroovyMBean> queryJmx(JmxClient jmxClient, String objNameStr) {
        return queryJmx(jmxClient, new ObjectName(objNameStr))
    }

    @PackageScope static List<GroovyMBean> queryJmx(JmxClient jmxClient, ObjectName objName) {
        List<ObjectName> names = jmxClient.query(objName)
        MBeanServerConnection server = jmxClient.connection
        return names.collect { new GroovyMBean(server, it) }
    }

    void fetch() {
        this.mbeans = []
        for(objectName in objectNames){
            def tmpMbeans = queryJmx(jmxClient, objectName)
            if (tmpMbeans.size() == 0) {
                logger.warning("Failed to fetch MBean ${objectName}.")
            } else {
                this.mbeans.addAll(tmpMbeans)
                logger.fine("Fetched ${tmpMbeans.size()} MBeans - ${tmpMbeans}")
            }
        }
    }

    @PackageScope List<GroovyMBean> getMBeans() {
        if (mbeans == null || mbeans.size() == 0) {
            logger.warning("No active MBeans.  Be sure to fetch() before updating any applicable instruments.")
            return []
        }
        return isSingle ? [mbeans[0]]: mbeans
    }

    @PackageScope List<Object> getAttribute(String attribute) {
        if (mbeans == null || mbeans.size() == 0) {
            return []
        }

        def ofInterest = isSingle ? [mbeans[0]]: mbeans
        return ofInterest.collect {
          getBeanAttribute(it, attribute)
        }
    }

    @PackageScope List<Tuple3<GroovyMBean, String, Object>> getAttributes(Set<String> attributes) {
        if (mbeans == null || mbeans.size() == 0) {
            return []
        }

        def ofInterest = isSingle ? [mbeans[0]]: mbeans
        return [ofInterest, attributes].combinations().collect { pair ->
            def (bean, attribute) = pair
            new Tuple3(bean, attribute, getBeanAttribute(bean, attribute))
        }
    }

    static Object getBeanAttribute(GroovyMBean bean, String attribute) {
        try {
            bean.getProperty(attribute)
        } catch (AttributeNotFoundException e) {
            logger.warning("Expected attribute ${attribute} not found in mbean ${bean.name()}")
            null
        }
    }
}
