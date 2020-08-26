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
        def serviceUrl = new JMXServiceURL('rmi', 'localhost', 0)
        jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceUrl, env, getPlatformMBeanServer())
        jmxServer.start()
        return jmxServer.getAddress()
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
