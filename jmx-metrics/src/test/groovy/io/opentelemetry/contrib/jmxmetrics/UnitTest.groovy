/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics

import spock.lang.Specification


class UnitTest extends Specification {

    void clearProperties() {
        // ensure relevant properties aren't set
        def properties = [
            JmxConfig.SERVICE_URL,
            JmxConfig.GROOVY_SCRIPT,
            JmxConfig.TARGET_SYSTEM,
            JmxConfig.INTERVAL_MILLISECONDS,
            JmxConfig.METRICS_EXPORTER_TYPE,
            JmxConfig.OTLP_ENDPOINT,
            JmxConfig.PROMETHEUS_HOST,
            JmxConfig.PROMETHEUS_PORT,
            JmxConfig.JMX_USERNAME,
            JmxConfig.JMX_PASSWORD,
            JmxConfig.JMX_REMOTE_PROFILE,
            JmxConfig.JMX_REALM
        ]
        properties.each {System.clearProperty(it)}
    }

    def setup() {
        clearProperties()
    }

    def cleanupSpec() {
        clearProperties()
    }
}
