/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
        targets = ["cassandra"]
        configureContainers('prometheus_config.properties', 0, 9123, useStdin)

        expect:
        when: 'we receive metrics from the prometheus endpoint'
        def scraped = receivedMetrics()

        then: 'they are of the expected format'
        scraped.size() == 46
        scraped[0].contains(
                '# HELP cassandra_current_tasks Number of tasks in queue with the given task status.')
        scraped[1].contains(
                '# TYPE cassandra_current_tasks gauge')
        scraped[2].contains(
                'cassandra_current_tasks{stage_name="MemtablePostFlush",task_status="PendingTasks"')

        cleanup:
        targetContainers.each { it.stop() }
        jmxExtensionAppContainer.stop()

        where:
        useStdin | _
        false | _
        true | _
    }
}
