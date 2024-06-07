/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThreadMatcherTest {

  private final ThreadMatcher threadMatcher = new ThreadMatcher();

  @Test
  void testLookup() {
    ArrayList<Thread> threads = new ArrayList<>();
    threadMatcher.forEachThread(
        new ThreadMatcher.NonCapturingPredicate<Thread, Void>() {
          @Override
          public boolean test(Thread thread, Void state) {
            return thread.getId() == Thread.currentThread().getId();
          }
        },
        null,
        new ThreadMatcher.NonCapturingConsumer<Thread, List<Thread>>() {
          @Override
          public void accept(Thread thread, List<Thread> state) {
            state.add(thread);
          }
        },
        threads);
    assertThat(threads).isEqualTo(Arrays.asList(Thread.currentThread()));
  }
}
