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


class JmxConfigTest extends UnitTest {

    def 'static values'() {
        expect: 'static values to be expected'
        JmxConfig.AVAILABLE_TARGET_SYSTEMS == ["jvm", "kafka", "cassandra"]
    }

    def 'default values'() {
        when: 'config is without system properties'
        def config = new JmxConfig()

        then:
        config.serviceUrl == null
        config.groovyScript == null
        config.targetSystem == ""
        config.intervalMilliseconds == 10000
        config.exporterType == "logging"
        config.otlpExporterEndpoint == null
        config.prometheusExporterHost == "localhost"
        config.prometheusExporterPort == 9090
        config.username == null
        config.password == null
        config.remoteProfile == null
        config.realm == null
    }

    def 'specified values'() {
        when: 'config is set via system properties'
        def properties = [
            "jmx.service.url" : "myServiceUrl",
            "jmx.groovy.script" : "myGroovyScript",
            "jmx.target.system" : "mytargetsystem",
            "jmx.interval.milliseconds": "123",
            "exporter": "inmemory",
            "exporter.otlp.endpoint": "myOtlpEndpoint",
            "exporter.prometheus.host": "myPrometheusHost",
            "exporter.prometheus.port": "234",
            "jmx.username": "myUsername",
            "jmx.password": "myPassword",
            "jmx.remote.profile": "myRemoteProfile",
            "jmx.realm": "myRealm"
        ]
        properties.each {System.setProperty("otel.${it.key}", it.value)}
        def config = new JmxConfig()

        then:
        config.serviceUrl == "myServiceUrl"
        config.groovyScript == "myGroovyScript"
        config.targetSystem == "mytargetsystem"
        config.intervalMilliseconds == 123
        config.exporterType == "inmemory"
        config.otlpExporterEndpoint == "myOtlpEndpoint"
        config.prometheusExporterHost == "myPrometheusHost"
        config.prometheusExporterPort == 234
        config.username == "myUsername"
        config.password == "myPassword"
        config.remoteProfile == "myRemoteProfile"
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

        where:
        prop | _
        'otel.jmx.interval.milliseconds' | _
        'otel.exporter.prometheus.port' | _
    }

    def 'conflicting groovy script and target system'() {
        setup: 'config is set with both groovy script and target system'
        [
            "service.url" : "requiredValue",
            "groovy.script": "myGroovyScript",
            "target.system": "myTargetSystem"
        ].each {
            System.setProperty("otel.jmx.${it.key}", it.value)
        }
        def config = new JmxConfig()

        def raised = null
        try {
            config.validate()
        } catch (ConfigurationException e) {
            raised = e
        }

        expect: 'config fails to validate'
        raised != null
        raised.message ==  "Only one of otel.jmx.groovy.script or otel.jmx.target.system can be specified."
    }

    def "invalid target system"() {
        setup: "config is set with nonexistant target system"
        [
            "service.url" : "requiredValue",
            "target.system": "unavailableTargetSystem"
        ].each {
            System.setProperty("otel.jmx.${it.key}", it.value)
        }
        def config = new JmxConfig()

        def raised = null
        try {
            config.validate()
        } catch (ConfigurationException e) {
            raised = e
        }

        expect: 'config fails to validate'
        raised != null
        raised.message ==  "unavailabletargetsystem must be one of [jvm, kafka, cassandra]"
    }
}
