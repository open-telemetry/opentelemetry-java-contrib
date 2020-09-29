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

import org.apache.hc.client5.http.fluent.Request
import spock.lang.Requires
import spock.lang.Timeout

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(60)
class JVMTargetSystemIntegrationTests extends IntegrationTest {

    def receivedMetrics() {
        def scraped = []
        for (int i = 0; i < 120; i++) {
            def resp = Request.get("http://localhost:${jmxExposedPort}/metrics").execute()
            def received = resp.returnContent().asString()
            if (received != '') {
                scraped = received.split('\n')
                if (scraped.size() > 2) {
                    break
                }
            }
            Thread.sleep(500)
        }
        return scraped
    }

    def 'end to end'() {
        setup: 'we configure JMX metrics gatherer and target server to use default JVM target system script'
        configureContainers('jvm_config.properties', 0, 9123, false)

        expect:
        when: 'we receive metrics from the prometheus endpoint'
        def scraped = receivedMetrics()

        then: 'they are of the expected format'
        scraped.size() == 3
        scraped[0].contains(
                '# HELP placeholder_metric For testing purposes')
        scraped[1].contains(
                '# TYPE placeholder_metric counter')
        scraped[2].contains(
                'placeholder_metric 1.0')

        cleanup:
        cassandraContainer.stop()
        jmxExtensionAppContainer.stop()
    }
}
