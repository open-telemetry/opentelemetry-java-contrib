/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import static java.lang.management.ManagementFactory.getPlatformMBeanServer
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import javax.management.ObjectName
import javax.management.remote.JMXConnectorServer
import javax.management.remote.JMXConnectorServerFactory
import javax.management.remote.JMXServiceURL
import spock.lang.Shared
import spock.lang.Specification

class OtelHelperJmxTest extends Specification {

    static String thingName = 'io.opentelemetry.extensions.metrics.jmx:type=OtelHelperJmxTest.Thing'

    @Shared
    JMXConnectorServer jmxServer

    def setupSpec() {
        def thing = new Thing()
        def mbeanServer = getPlatformMBeanServer()
        mbeanServer.registerMBean(thing, new ObjectName(thingName))
    }

    def cleanup() {
        jmxServer.stop()
    }

    private JMXServiceURL setupServer(Map env) {
        def serviceUrl = new JMXServiceURL('rmi', 'localhost', availablePort())
        jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, env, getPlatformMBeanServer())
        jmxServer.start()
        return jmxServer.getAddress()
    }

    private static def availablePort() {
      def sock = new ServerSocket(0);
      def port = sock.getLocalPort()
      sock.close()
      return port
    }

    private OtelHelper setupHelper(JmxConfig config) {
        return new OtelHelper(new JmxClient(config), new GroovyMetricEnvironment(config))
    }

    private void verifyClient(Properties props) {
        props.setProperty(JmxConfig.GROOVY_SCRIPT, "myscript.groovy")
        def config = new JmxConfig(props)
        config.validate()
        def otel = setupHelper(config)
        def mbeans = otel.queryJmx(thingName)

        assertEquals(1, mbeans.size())
        assertEquals('This is the attribute', mbeans[0].SomeAttribute)
    }

    def "no authentication"() {
        when:
        def serverAddr = setupServer([:])
        def props = new Properties().tap {
            it.setProperty(JmxConfig.SERVICE_URL, "${serverAddr}")
        }
        then:
        verifyClient(props)
    }

    def "password authentication"() {
        when:
        def pwFile = ClassLoader.getSystemClassLoader().getResource('jmxremote.password').getPath()
        def serverAddr = setupServer(['jmx.remote.x.password.file':pwFile])

        def props = new Properties().tap {
            it.setProperty('otel.jmx.service.url', "${serverAddr}")
            it.setProperty(JmxConfig.JMX_USERNAME, 'wrongUsername')
            it.setProperty(JmxConfig.JMX_PASSWORD, 'wrongPassword')
        }

        then:
        try {
            verifyClient(props)
            assertTrue('Authentication should have failed.', false)
        } catch (final SecurityException e) {
            // desired
        }

        when:
        props = new Properties().tap {
            it.setProperty(JmxConfig.SERVICE_URL, "${serverAddr}")
            it.setProperty(JmxConfig.JMX_USERNAME, 'correctUsername')
            it.setProperty(JmxConfig.JMX_PASSWORD, 'correctPassword')
        }
        then:
        verifyClient(props)
    }

    def "sorted query results"() {
        when:
        def serverAddr = setupServer([:])
        def config = new JmxConfig(new Properties().tap {
            it.setProperty(JmxConfig.SERVICE_URL, "${serverAddr}")
            it.setProperty(JmxConfig.GROOVY_SCRIPT, "myscript.groovy")
        })
        config.validate()
        def otel = setupHelper(config)

        def things = (0..99).collect {new Thing()}

        def mbeanServer = getPlatformMBeanServer()
        things.eachWithIndex { thing, i ->
            mbeanServer.registerMBean(thing, new ObjectName("sorted.query.results:type=Thing,thing=${i}"))
        }

        then:
        def mbeans = otel.queryJmx("sorted.query.results:type=Thing,*")
        assertEquals(100, mbeans.size())

        def names = mbeans.collect { it.name() as String }
        def sortedNames = names.collect().sort()
        assertEquals(sortedNames, names)
    }

    def "multiple objectname query"() {
        when:
        def serverAddr = setupServer([:])
        def config = new JmxConfig(new Properties().tap {
            it.setProperty(JmxConfig.SERVICE_URL, "${serverAddr}")
            it.setProperty(JmxConfig.GROOVY_SCRIPT, "myscript.groovy")
        })
        config.validate()
        def otel = setupHelper(config)

        def things = (0..99).collect {new Thing()}

        def mbeanServer = getPlatformMBeanServer()
        things.eachWithIndex { thing, i ->
            mbeanServer.registerMBean(thing, new ObjectName("multiobjectname.query.results:type=Thing,thing=${i}"))
        }

        then:
        def mbeanHelper = otel.mbeans([
            "multiobjectname.query.results:type=Thing,thing=1",
            "multiobjectname.query.results:type=Thing,thing=2"
        ])
        def mbeans = mbeanHelper.getMBeans()
        assertEquals(2, mbeans.size())
    }

    interface ThingMBean {

        String getSomeAttribute()
    }

    static class Thing implements ThingMBean {

        @Override
        String getSomeAttribute() {
            return 'This is the attribute'
        }
    }
}
