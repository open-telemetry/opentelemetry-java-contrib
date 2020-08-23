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
import spock.lang.Requires;

@Requires({
    System.getProperty('ojc.integration.tests') == 'true'
})
class PrometheusIntegrationTests extends IntegrationTest {

    def setupSpec() {
        configureContainers('prometheus_config.properties', 9123)
    }

    def receivedMetrics() {
        def received = ''
        while (received == '' || received.charAt(0) != '#') {
            Thread.sleep(500)
            received = Request.get("http://localhost:${jmxExposedPort}/metrics").execute().returnContent().asString()
        }
        return received
    }

    def 'end to end'() {
        when: 'we receive metrics from the prometheus endpoint'
        def scraped = receivedMetrics().split('\n')

        then: 'they are of the expected format'
        scraped.size() == 6
        scraped[0].contains(
                '# HELP jmx_metrics_cassandra_storage_load Size, in bytes, of the on disk data size this node manages')
        scraped[1].contains(
                '# TYPE jmx_metrics_cassandra_storage_load summary')
        scraped[2].contains(
                'jmx_metrics_cassandra_storage_load_count{myKey="myVal",} ')
        scraped[3].contains(
                'jmx_metrics_cassandra_storage_load_sum{myKey="myVal",} ')
        scraped[4].contains(
                'jmx_metrics_cassandra_storage_load{myKey="myVal",quantile="0.0",} ')

        scraped[5].contains(
                'jmx_metrics_cassandra_storage_load{myKey="myVal",quantile="100.0",} ')
    }
}
