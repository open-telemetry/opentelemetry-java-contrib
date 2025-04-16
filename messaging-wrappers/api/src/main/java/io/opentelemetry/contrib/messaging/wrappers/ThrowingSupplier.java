/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers;

import java.util.function.Supplier;

/**
 * A utility interface representing a {@link Supplier} that may throw.
 *
 * <p>Inspired from <a
 * href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/testing-common/src/main/java/io/opentelemetry/instrumentation/testing/util/ThrowingSupplier.java>ThrowingSupplier</a>.
 *
 * @param <E> Thrown exception type.
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
  T get() throws E;
}
