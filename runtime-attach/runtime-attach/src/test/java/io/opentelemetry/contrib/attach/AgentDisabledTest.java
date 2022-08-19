/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

public class AgentDisabledTest extends AbstractAttachmentTest {

  @SetEnvironmentVariable(key = "OTEL_JAVAAGENT_ENABLED", value = "false")
  @Test
  void shouldNotAttachWhenAgentDisabledWithEnvVariable() {
    RuntimeAttach.attachJavaagentToCurrentJVM();
    verifyNoAttachment();
  }

  @WithSpan
  void verifyNoAttachment() {
    assertThat(isAttached()).as("Agent should not be attached").isFalse();
  }

  @SetSystemProperty(key = "otel.javaagent.enabled", value = "false")
  @Test
  void shouldNotAttachWhenAgentDisabledWithProperty() {
    RuntimeAttach.attachJavaagentToCurrentJVM();
    verifyNoAttachment();
  }
}
