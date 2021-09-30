/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import spock.lang.Unroll

import static java.lang.management.ManagementFactory.getPlatformMBeanServer

import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.remote.JMXConnectorServer
import javax.management.remote.JMXConnectorServerFactory
import javax.management.remote.JMXServiceURL
import spock.lang.Shared
import spock.lang.Specification


class MBeanHelperTest extends Specification {

    @Shared
    MBeanServer mBeanServer

    @Shared
    JMXConnectorServer jmxServer

    @Shared
    JmxClient jmxClient

    def setup() {
        mBeanServer = getPlatformMBeanServer()

        def serviceUrl = new JMXServiceURL('rmi', 'localhost', 0)
        jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, [:], mBeanServer)
        jmxServer.start()
        def completeAddress = jmxServer.getAddress()

        def props = new Properties().tap {
            it.setProperty(JmxConfig.SERVICE_URL, "${completeAddress}")
        }

        def jmxConfig = new JmxConfig(props)
        jmxClient = new JmxClient(jmxConfig)
    }

    def cleanup() {
        jmxServer.stop()
    }

    @Unroll
    def "represents #quantity MBean(s)"() {
        setup:
        def thingName = "io.opentelemetry.contrib.jmxmetrics:type=${quantity}Thing"
        def things = (0..100).collect { new Thing(it as String) }
        things.eachWithIndex { thing, i ->
            def name = "${thingName},thing=${i}"
            mBeanServer.registerMBean(thing, new ObjectName(name))
        }

        expect:
        when: "We create and register 100 Things and create ${quantity} MBeanHelper"
        def mbeanHelper = new MBeanHelper(jmxClient, "${thingName},*", isSingle)
        mbeanHelper.fetch()

        then: "${quantity} returned"
        def returned = mbeanHelper.getAttribute("SomeAttribute")
        returned == isSingle ? ["0"]: (0..100).collect {it as String}.sort()

        where:
        isSingle | quantity
        true     | "single"
        false    | "multiple"
    }

    @Unroll
    def "handles missing attributes"() {
        setup:
        def thingName = "io.opentelemetry.contrib.jmxmetrics:type=${quantity}MissingAttributeThing"
        def things = (0..100).collect { new Thing(it as String) }
        things.eachWithIndex { thing, i ->
            def name = "${thingName},thing=${i}"
            mBeanServer.registerMBean(thing, new ObjectName(name))
        }

        expect:
        when: "We request a nonexistent attribute via MBeanHelper"
        def mbeanHelper = new MBeanHelper(jmxClient, "${thingName},*", isSingle)
        mbeanHelper.fetch()

        then: "nulls are returned"
        def returned = mbeanHelper.getAttribute("MissingAttribute")
        returned == isSingle ? [null]: (0..100).collect {null}

        where:
        isSingle | quantity
        true     | "single"
        false    | "multiple"
    }

    interface ThingMBean {

        String getSomeAttribute()
    }

    static class Thing implements ThingMBean {

        private String attrValue

        Thing(attrValue) {
            this.attrValue = attrValue
        }

        @Override
        String getSomeAttribute() {
            return attrValue
        }
    }
}
