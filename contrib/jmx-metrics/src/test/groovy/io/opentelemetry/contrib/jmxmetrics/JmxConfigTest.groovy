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

import spock.lang.Specification


class JmxConfigTest extends Specification {

    void clearProperties() {
        // ensure relevant properties aren't set
        def properties = [
            "jmx.service.url",
            "jmx.groovy.script",
            "jmx.interval.milliseconds",
            "exporter",
            "otlp.endpoint",
            "prometheus.host",
            "prometheus.port",
            "jmx.username",
            "jmx.password",
            "jmx.remote.profiles",
            "jmx.realm"
        ]
        properties.each {System.clearProperty("otel.${it}")}
    }

    def setup() {
        clearProperties()
    }

    def cleanupSpec() {
        clearProperties()
    }

    def 'default values'() {
        when: 'config is without system properties'
        def config = new JmxConfig()

        then:
        config.serviceUrl == ""
        config.groovyScript == ""
        config.intervalMilliseconds == 10000
        config.exporterType == "logging"
        config.otlpExporterEndpoint == ""
        config.prometheusExporterHost == "localhost"
        config.prometheusExporterPort == 9090
        config.username == ""
        config.password == ""
        config.remoteProfiles == ""
        config.realm == ""
    }

    def 'specified values'() {
        when: 'config is set via system properties'
        def properties = [
            "jmx.service.url" : "myServiceUrl",
            "jmx.groovy.script" : "myGroovyScript",
            "jmx.interval.milliseconds": "123",
            "exporter": "inmemory",
            "otlp.endpoint": "myOtlpEndpoint",
            "prometheus.host": "myPrometheusHost",
            "prometheus.port": "234",
            "jmx.username": "myUsername",
            "jmx.password": "myPassword",
            "jmx.remote.profiles": "myRemoteProfile",
            "jmx.realm": "myRealm"
        ]
        properties.each {System.setProperty("otel.${it.key}", it.value)}
        def config = new JmxConfig()

        then:
        config.serviceUrl == "myServiceUrl"
        config.groovyScript == "myGroovyScript"
        config.intervalMilliseconds == 123
        config.exporterType == "inmemory"
        config.otlpExporterEndpoint == "myOtlpEndpoint"
        config.prometheusExporterHost == "myPrometheusHost"
        config.prometheusExporterPort == 234
        config.username == "myUsername"
        config.password == "myPassword"
        config.remoteProfiles == "myRemoteProfile"
        config.realm == "myRealm"
    }

    def 'invalid values'() {
        setup: 'config is set with invalid interval'
        System.setProperty(prop, 'abc')
        def raised = null
        try {
            new JmxConfig()
        } catch (ConfigurationException e) {
            raised = e
        }

        expect: 'config fails to be created'
        raised != null
        raised.message ==  "Failed to parse ${prop}"
        println "JmxConfigTest.invalid values: ${raised.message}"

        where:
        prop | _
        'otel.jmx.interval.milliseconds' | _
        'otel.prometheus.port' | _
    }
}
