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
            "jmx.service.url",
            "jmx.groovy.script",
            "jmx.target.system",
            "jmx.interval.milliseconds",
            "exporter",
            "otlp.endpoint",
            "prometheus.host",
            "prometheus.port",
            "jmx.username",
            "jmx.password",
            "jmx.remote.profile",
            "jmx.realm"
        ]
        properties.each {System.clearProperty("otel.${it}")}
    }

    def setup() {
        clearProperties()
    }

    def cleanupSpec() {
        clearProperties()
    }
}
