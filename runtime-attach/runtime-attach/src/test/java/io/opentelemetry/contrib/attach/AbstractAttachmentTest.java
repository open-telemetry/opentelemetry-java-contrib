/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.BeforeAll;

class AbstractAttachmentTest {

  @BeforeAll
  static void disableMainThreadCheck() {
    System.setProperty("otel.javaagent.testing.runtime-attach.main-method-check", "false");
  }

  boolean isAttached() {
    return Span.current().getSpanContext().isValid();
  }
}
