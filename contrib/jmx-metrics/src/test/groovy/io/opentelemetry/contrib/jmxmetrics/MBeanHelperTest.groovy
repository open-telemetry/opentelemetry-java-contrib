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
        println "MBeanHelperTest.represents #quantity MBean(s): ${returned}"
        returned == isSingle ? ["0"]: (0..100).collect {it as String}.sort()

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
