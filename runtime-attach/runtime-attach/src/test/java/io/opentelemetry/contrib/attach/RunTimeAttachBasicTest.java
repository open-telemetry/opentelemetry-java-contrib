/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.junit.jupiter.api.Test;

public class RunTimeAttachBasicTest extends AbstractAttachmentTest {

  @Test
  void shouldAttach() {
    RuntimeAttach.attachJavaagentToCurrentJVM();
    verifyAttachment();
  }

  @WithSpan
  void verifyAttachment() {
    assertThat(isAttached()).as("Agent should be attached").isTrue();
  }
}
