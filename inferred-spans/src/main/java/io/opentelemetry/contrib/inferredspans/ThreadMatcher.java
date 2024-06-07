/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

public class ThreadMatcher {

  private final ThreadGroup systemThreadGroup;
  private Thread[] threads = new Thread[16];

  public ThreadMatcher() {
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    while (threadGroup.getParent() != null) {
      threadGroup = threadGroup.getParent();
    }
    systemThreadGroup = threadGroup;
  }

  public <S1, S2> void forEachThread(
      NonCapturingPredicate<Thread, S1> predicate,
      S1 state1,
      NonCapturingConsumer<Thread, S2> consumer,
      S2 state2) {
    int count = systemThreadGroup.activeCount();
    do {
      int expectedArrayLength = count + (count / 2) + 1;
      if (threads.length < expectedArrayLength) {
        threads = new Thread[expectedArrayLength]; // slightly grow the array size
      }
      count = systemThreadGroup.enumerate(threads, true);
      // return value of enumerate() must be strictly less than the array size according to javadoc
    } while (count >= threads.length);

    for (int i = 0; i < count; i++) {
      Thread thread = threads[i];
      if (predicate.test(thread, state1)) {
        consumer.accept(thread, state2);
      }
      threads[i] = null;
    }
  }

  interface NonCapturingPredicate<T, S> {
    boolean test(T t, S state);
  }

  interface NonCapturingConsumer<T, S> {
    void accept(T t, S state);
  }
}
