/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.jmxmetrics

import javax.management.ObjectName

class GroovyRunnerTest extends UnitTest {

    def 'target system scripts are loaded from resources'() {
        when: 'available target system script is used'
        System.setProperty('otel.jmx.service.url', 'service:jmx:rmi:///jndi/rmi://localhost:12345/jmxrmi')
        System.setProperty('otel.jmx.target.system', 'jvm')
        def config = new JmxConfig()
        config.validate()

        def exportCalled = false

        def stub = new JmxClient(config) {
                    @Override
                    List<ObjectName> query(final ObjectName objectName) {
                        return [] as List<ObjectName>;
                    }
                }

        def groovyRunner = new GroovyRunner(config, stub, new GroovyMetricEnvironment(config) {
                    @Override
                    void exportMetrics() {
                        exportCalled = true
                    }
                })

        then: 'it is successfully loaded and runnable'
        groovyRunner.scripts.size() == 1
        groovyRunner.run()
        exportCalled
    }

    def 'unavailable target system scripts are attempted to be loaded'() {
        when: 'unavailable target system script is used'
        System.setProperty('otel.jmx.service.url', 'requiredValue')
        System.setProperty('otel.jmx.target.system', 'notAProvidededTargetSystem')
        def config = new JmxConfig()

        then: 'it fails to successfully load'
        def raised = null
        try {
            def groovyRunner = new GroovyRunner(config, null, null)
        } catch (ConfigurationException e) {
            raised = e
        }
        raised != null
        raised.message == 'Failed to load target-systems/notaprovidededtargetsystem.groovy'
    }
}
