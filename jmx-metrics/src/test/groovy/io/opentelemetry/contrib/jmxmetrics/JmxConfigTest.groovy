/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics


class JmxConfigTest extends UnitTest {

    def 'static values'() {
        expect: 'static values to be expected'
        JmxConfig.AVAILABLE_TARGET_SYSTEMS == [
            "cassandra",
            "jvm",
            "kafka",
            "kafka-consumer",
            "kafka-producer"
        ]
    }

    def 'default values'() {
        when: 'config is without system properties'
        def config = new JmxConfig()

        then:
        config.serviceUrl == null
        config.groovyScript == null
        config.targetSystem == ""
        config.targetSystems == [] as LinkedHashSet
        config.intervalMilliseconds == 10000
        config.metricsExporterType == "logging"
        config.otlpExporterEndpoint == null
        config.prometheusExporterHost == "0.0.0.0"
        config.prometheusExporterPort == 9464
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
            "jmx.target.system" : "mytargetsystem,mytargetsystem,myothertargetsystem,myadditionaltargetsystem",
            "jmx.interval.milliseconds": "123",
            "metrics.exporter": "inmemory",
            "exporter.otlp.endpoint": "https://myOtlpEndpoint",
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
        config.targetSystem == "mytargetsystem,mytargetsystem,myothertargetsystem,myadditionaltargetsystem"
        config.targetSystems == [
            "mytargetsystem",
            "myothertargetsystem",
            "myadditionaltargetsystem"
        ] as LinkedHashSet
        config.intervalMilliseconds == 123
        config.metricsExporterType == "inmemory"
        config.otlpExporterEndpoint == "https://myOtlpEndpoint"
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
            "target.system": "jvm,unavailableTargetSystem"
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
        raised.message ==  "[jvm, unavailabletargetsystem] must specify targets from [cassandra, jvm, kafka, kafka-consumer, kafka-producer]"
    }
}
