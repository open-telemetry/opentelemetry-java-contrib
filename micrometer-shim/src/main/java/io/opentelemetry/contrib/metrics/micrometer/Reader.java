/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.metrics.micrometer;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
interface Reader<T> extends Supplier<T> {
  default Reader<T> andThen(Function<T, T> fn) {
    return () -> fn.apply(get());
  }
}
