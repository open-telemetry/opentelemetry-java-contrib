/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class GroovyRunnerTest {

  @Test
  @SetSystemProperty(
      key = "otel.jmx.service.url",
      value = "service:jmx:rmi:///jndi/rmi://localhost:12345/jmxrmi")
  @SetSystemProperty(key = "otel.jmx.target.system", value = "jvm")
  void loadTargetScript() throws Exception {
    JmxConfig config = new JmxConfig();

    assertThatCode(config::validate).doesNotThrowAnyException();

    AtomicBoolean exportCalled = new AtomicBoolean();

    JmxClient stub =
        new JmxClient(config) {
          @Override
          public List<ObjectName> query(ObjectName objectName) {
            return Collections.emptyList();
          }
        };

    GroovyRunner runner =
        new GroovyRunner(
            config,
            stub,
            new GroovyMetricEnvironment(config) {
              @Override
              public void exportMetrics() {
                exportCalled.set(true);
              }
            });

    assertThat(runner.getScripts()).hasSize(1);
    runner.run();
    assertThat(exportCalled).isTrue();
  }

  @Test
  @SetSystemProperty(key = "otel.jmx.service.url", value = "requiredValue")
  @SetSystemProperty(key = "otel.jmx.target.system", value = "notAProvidededTargetSystem")
  void loadUnavailableTargetScript() throws Exception {
    JmxConfig config = new JmxConfig();

    assertThatThrownBy(() -> new GroovyRunner(config, null, null))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("Failed to load target-systems/notaprovidededtargetsystem.groovy");
  }
}
