/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.asyncprofiler;

import static io.opentelemetry.contrib.inferredspans.WildcardMatcher.caseSensitiveMatcher;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.inferredspans.internal.StackFrame;
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
    // This test tries to decode the recording.jfr file provided as a resource.
    // To regenerate the test file, run SamplingProfilerTest.testProfileTransaction with the
    // backupDiagnosticFiles config option set to "true"
    // Then replace the recording.jfr file in the resources folder with the one from the test-run
    // Note that the number of actual samples in the JFR file may vary, so you'll need to adjust the
    // assertions
    // in this test

    // Using a small buffer, but big enough to fit the largest string in the JFR file to test edge
    // cases
    // This size maybe needs to be increased after regenerating the JFR file
    JfrParser jfrParser = new JfrParser(ByteBuffer.allocate(368), ByteBuffer.allocate(368));

    File file =
        Paths.get(JfrParserTest.class.getClassLoader().getResource("recording.jfr").toURI())
            .toFile();

    jfrParser.parse(
        file,
        Collections.emptyList(),
        Collections.singletonList(
            caseSensitiveMatcher("io.opentelemetry.contrib.inferredspans.*")));
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
    assertThat(stackTraces.get()).isEqualTo(92);
  }

  @Test
  void testParseEmptyFile() throws Exception {
    File file = File.createTempFile("empty", ".jfr");
    try {
      JfrParser jfrParser = new JfrParser();
      jfrParser.parse(file, Collections.emptyList(), Collections.emptyList());
      jfrParser.consumeStackTraces((threadId, stackTraceId, nanoTime) -> {});
    } finally {
      file.delete();
    }
  }
}
