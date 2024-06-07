/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.asyncprofiler;

import static io.opentelemetry.contrib.inferredspans.config.WildcardMatcher.caseSensitiveMatcher;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.inferredspans.StackFrame;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JfrParserTest {

  private static final int MAX_STACK_DEPTH = 4;

  @Test
  void name() throws Exception {
    // Using a small buffer, but big enough to fit the largest string in the JFR file to test edge
    // cases
    JfrParser jfrParser = new JfrParser(ByteBuffer.allocate(368), ByteBuffer.allocate(368));

    File file =
        Paths.get(JfrParserTest.class.getClassLoader().getResource("recording.jfr").toURI())
            .toFile();

    jfrParser.parse(
        file,
        Collections.emptyList(),
        Collections.singletonList(caseSensitiveMatcher("co.elastic.otel.*")));
    AtomicInteger stackTraces = new AtomicInteger();
    ArrayList<StackFrame> stackFrames = new ArrayList<>();
    jfrParser.consumeStackTraces(
        (threadId, stackTraceId, nanoTime) -> {
          jfrParser.resolveStackTrace(stackTraceId, stackFrames, MAX_STACK_DEPTH);
          if (!stackFrames.isEmpty()) {
            stackTraces.incrementAndGet();
            assertThat(stackFrames.get(stackFrames.size() - 1).getMethodName())
                .isEqualTo("testProfileTransaction");
            assertThat(stackFrames).hasSizeLessThanOrEqualTo(MAX_STACK_DEPTH);
          }
          stackFrames.clear();
        });
    assertThat(stackTraces.get()).isEqualTo(98);
  }
}
