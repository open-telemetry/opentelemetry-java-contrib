/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.junit.jupiter.api.Test;

public class AgentDisabledByEnvironmentVariableTest extends AbstractAttachmentTest {

  @Test
  void shouldNotAttachWhenAgentDisabledWithEnvVariable() {
    RuntimeAttach.attachJavaagentToCurrentJvm();
    verifyNoAttachment();
  }

  @WithSpan
  void verifyNoAttachment() {
    assertThat(isAttached()).as("Agent should not be attached").isFalse();
  }
}
