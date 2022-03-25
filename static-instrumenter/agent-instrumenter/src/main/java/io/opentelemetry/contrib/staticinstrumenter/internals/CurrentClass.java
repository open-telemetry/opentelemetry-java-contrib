/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.internals;

public class CurrentClass {

  private static final ThreadLocal<TransformedClass> currentClass = new ThreadLocal<>();

  private CurrentClass() {}

  static TransformedClass getAndRemove() {
    TransformedClass tc = currentClass.get();
    currentClass.remove();
    return tc;
  }

  static void set(TransformedClass clazz) {
    currentClass.set(clazz);
  }
}
