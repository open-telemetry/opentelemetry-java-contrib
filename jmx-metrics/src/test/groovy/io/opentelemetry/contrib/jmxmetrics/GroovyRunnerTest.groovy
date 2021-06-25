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
