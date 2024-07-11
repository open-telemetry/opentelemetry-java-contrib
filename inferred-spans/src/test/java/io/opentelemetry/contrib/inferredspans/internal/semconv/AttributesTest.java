/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.semconv;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import org.junit.jupiter.api.Test;

public class AttributesTest {

  @Test
  public void checkCodeStacktraceUpToDate() {
    assertThat(Attributes.CODE_STACKTRACE).isEqualTo(CodeIncubatingAttributes.CODE_STACKTRACE);
  }
}
