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


class UnitTest extends Specification {

    void clearProperties() {
        // ensure relevant properties aren't set
        def properties = [
            JmxConfig.SERVICE_URL,
            JmxConfig.GROOVY_SCRIPT,
            JmxConfig.TARGET_SYSTEM,
            JmxConfig.INTERVAL_MILLISECONDS,
            JmxConfig.EXPORTER_TYPE,
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
