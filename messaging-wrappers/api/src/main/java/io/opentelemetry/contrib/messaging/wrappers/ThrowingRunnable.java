package io.opentelemetry.contrib.messaging.wrappers;

/**
 * A utility interface representing a {@link Runnable} that may throw.
 *
 * <p>Inspired from <a href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/testing-common/src/main/java/io/opentelemetry/instrumentation/testing/util/ThrowingRunnable.java>ThrowingRunnable</a>.
 *
 * @param <E> Thrown exception type.
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
  void run() throws E;
}
