/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.attach;

import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Span;

public class AbstractAttachmentTest {

  boolean isAttached() {
    return Span.current().getSpanContext().isValid();
  }
}
