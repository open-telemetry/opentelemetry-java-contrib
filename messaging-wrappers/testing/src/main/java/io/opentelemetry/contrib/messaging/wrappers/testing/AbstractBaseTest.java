package io.opentelemetry.contrib.messaging.wrappers.testing;

import io.opentelemetry.contrib.messaging.wrappers.testing.internal.AutoConfiguredDataCapture;
import io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.awaitility.core.ConditionTimeoutException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil.orderByRootSpanName;
import static io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil.waitForTraces;
import static org.awaitility.Awaitility.await;

public abstract class AbstractBaseTest {

  public static Comparator<List<SpanData>> sortByRootSpanName(String... names) {
    return orderByRootSpanName(names);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static void waitAndAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      Consumer<TraceAssert>... assertions) {
    List<Consumer<TraceAssert>> assertionsList = new ArrayList<>(Arrays.asList(assertions));
    try {
      await()
          .untilAsserted(() -> doAssertTraces(traceComparator, AutoConfiguredDataCapture::getSpans, assertionsList));
    } catch (Throwable t) {
      // awaitility is doing a jmx call that is not implemented in GraalVM:
      // call:
      // https://github.com/awaitility/awaitility/blob/fbe16add874b4260dd240108304d5c0be84eabc8/awaitility/src/main/java/org/awaitility/core/ConditionAwaiter.java#L157
      // see https://github.com/oracle/graal/issues/6101 (spring boot graal native image)
      if (t.getClass().getName().equals("com.oracle.svm.core.jdk.UnsupportedFeatureError")
          || t instanceof ConditionTimeoutException) {
        // Don't throw this failure since the stack is the awaitility thread, causing confusion.
        // Instead, just assert one more time on the test thread, which will fail with a better
        // stack trace.
        // TODO(anuraaga): There is probably a better way to do this.
        doAssertTraces(traceComparator, AutoConfiguredDataCapture::getSpans, assertionsList);
      } else {
        throw t;
      }
    }
  }

  public static void doAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      Supplier<List<SpanData>> supplier,
      List<Consumer<TraceAssert>> assertionsList) {
    try {
      List<List<SpanData>> traces = waitForTraces(supplier, assertionsList.size());
      TelemetryDataUtil.assertScopeVersion(traces);
      if (traceComparator != null) {
        traces.sort(traceComparator);
      }
      TracesAssert.assertThat(traces).hasTracesSatisfyingExactly(assertionsList);
    } catch (InterruptedException | TimeoutException e) {
      throw new AssertionError("Error waiting for " + assertionsList.size() + " traces", e);
    }
  }
}
