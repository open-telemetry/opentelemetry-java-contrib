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
import spock.lang.Unroll

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
@Timeout(60)
class PrometheusIntegrationTests extends IntegrationTest {

    def receivedMetrics() {
        def scraped = []
        for (int i = 0; i < 120; i++) {
            def received = Request.get("http://localhost:${jmxExposedPort}/metrics").execute().returnContent().asString()
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

    @Unroll
    def 'end to end with stdin config: #useStdin'() {
        setup: 'we configure JMX metrics gatherer and target server'
        configureContainers('prometheus_config.properties', 0, 9123, useStdin)

        expect:
        when: 'we receive metrics from the prometheus endpoint'
        def scraped = receivedMetrics()

        then: 'they are of the expected format'
        scraped.size() == 6
        scraped[0].contains(
                '# HELP cassandra_storage_load Size, in bytes, of the on disk data size this node manages')
        scraped[1].contains(
                '# TYPE cassandra_storage_load summary')
        scraped[2].contains(
                'cassandra_storage_load_count{myKey="myVal",} ')
        scraped[3].contains(
                'cassandra_storage_load_sum{myKey="myVal",} ')
        scraped[4].contains(
                'cassandra_storage_load{myKey="myVal",quantile="0.0",} ')
        scraped[5].contains(
                'cassandra_storage_load{myKey="myVal",quantile="100.0",} ')

        cleanup:
        cassandraContainer.stop()
        jmxExtensionAppContainer.stop()

        where:
        useStdin | _
        false | _
        true | _
    }
}
