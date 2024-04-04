/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace.util;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class MutableSpanTest {

  @Test
  public void noSpanDataCopyWithoutMutation() {
    ReadableSpan original = createSpan("foo", builder -> {});

    MutableSpan mutable = MutableSpan.makeMutable(original);
    SpanData first = mutable.toSpanData();
    SpanData second = mutable.toSpanData();

    assertThat(first.getClass().getName())
        .isEqualTo("io.opentelemetry.sdk.trace.AutoValue_SpanWrapper");
    assertThat(first).isSameAs(second);
  }

  @Test
  public void freezeAfterMutation() {
    ReadableSpan original = createSpan("foo", builder -> {});

    MutableSpan mutable1 = MutableSpan.makeMutable(original);
    mutable1.setName("updated");
    mutable1.toSpanData();

    assertThatThrownBy(() -> mutable1.setName("should not be allowed"))
        .isInstanceOf(IllegalStateException.class);

    // it should be okay to wrap again and then mutate
    MutableSpan mutable2 = MutableSpan.makeMutable(mutable1);
    mutable2.setName("updated again");

    assertThat(mutable1.toSpanData()).hasName("updated");
    assertThat(mutable2.toSpanData()).hasName("updated again");

    Assertions.assertThat(mutable1.getOriginalSpan()).isSameAs(original);
    Assertions.assertThat(mutable2.getOriginalSpan()).isSameAs(mutable1);
  }

  @Test
  public void testAttributesMutations() {
    AttributeKey<String> keep = AttributeKey.stringKey("keep-me");
    AttributeKey<String> update = AttributeKey.stringKey("update-me");
    AttributeKey<String> remove = AttributeKey.stringKey("remove-me");
    AttributeKey<String> add = AttributeKey.stringKey("add-me");

    ReadableSpan original =
        createSpan(
            "foo",
            builder -> {
              builder.setAttribute(keep, "keep-original");
              builder.setAttribute(update, "update-original");
              builder.setAttribute(remove, "remove-original");
            });

    MutableSpan mutable = MutableSpan.makeMutable(original);

    Assertions.assertThat(mutable.getAttribute(keep)).isEqualTo("keep-original");
    Assertions.assertThat(mutable.getAttribute(update)).isEqualTo("update-original");
    Assertions.assertThat(mutable.getAttribute(remove)).isEqualTo("remove-original");

    mutable.setAttribute(add, "added");
    mutable.removeAttribute(remove);
    mutable.setAttribute(update, "updated");

    Assertions.assertThat(mutable.getAttribute(keep)).isEqualTo("keep-original");
    Assertions.assertThat(mutable.getAttribute(update)).isEqualTo("updated");
    Assertions.assertThat(mutable.getAttribute(remove)).isNull();
    Assertions.assertThat(mutable.getAttribute(add)).isEqualTo("added");

    // check again after the MutableSpan has been frozen due ot the toSpanData() call
    assertThat(mutable.toSpanData().getAttributes())
        .hasSize(3)
        .containsEntry(keep, "keep-original")
        .containsEntry(update, "updated")
        .containsEntry(add, "added");

    Assertions.assertThat(mutable.getAttribute(keep)).isEqualTo("keep-original");
    Assertions.assertThat(mutable.getAttribute(update)).isEqualTo("updated");
    Assertions.assertThat(mutable.getAttribute(remove)).isNull();
    Assertions.assertThat(mutable.getAttribute(add)).isEqualTo("added");

    // Ensure attributes are cached
    assertThat(mutable.toSpanData().getAttributes()).isSameAs(mutable.toSpanData().getAttributes());
  }

  @Test
  public void testAttributesReusedIfNotMutated() {
    AttributeKey<String> key = AttributeKey.stringKey("first-key");
    AttributeKey<String> cancelledKey = AttributeKey.stringKey("second-key");

    ReadableSpan original =
        createSpan(
            "foo",
            builder -> {
              builder.setAttribute(key, "original");
            });

    MutableSpan mutable1 = MutableSpan.makeMutable(original);
    mutable1.setAttribute(key, "updated");
    mutable1.setAttribute(cancelledKey, "removed later");
    mutable1.setAttribute(key, "original");
    mutable1.removeAttribute(cancelledKey);

    SpanData mutatedSpanData = mutable1.toSpanData();

    assertThat(mutatedSpanData.getAttributes()).isSameAs(original.toSpanData().getAttributes());
  }

  @Test
  public void noDoubleWrapping() {
    ReadableSpan original = createSpan("foo", builder -> {});

    MutableSpan mutable = MutableSpan.makeMutable(original);
    Assertions.assertThat(MutableSpan.makeMutable(mutable)).isSameAs(mutable);

    mutable.setName("updated");
    Assertions.assertThat(MutableSpan.makeMutable(mutable)).isSameAs(mutable);
  }

  private static ReadableSpan createSpan(String name, Consumer<SpanBuilder> spanCustomizer) {

    AtomicReference<ReadableSpan> resultSpan = new AtomicReference<>();
    SpanProcessor collecting =
        new SpanProcessor() {
          @Override
          public void onStart(Context parentContext, ReadWriteSpan span) {}

          @Override
          public boolean isStartRequired() {
            return false;
          }

          @Override
          public void onEnd(ReadableSpan span) {
            resultSpan.set(span);
          }

          @Override
          public boolean isEndRequired() {
            return true;
          }
        };

    try (OpenTelemetrySdk sdk = TestUtils.sdkWith(collecting)) {

      SpanBuilder builder = sdk.getTracer("my-tracer").spanBuilder(name);
      spanCustomizer.accept(builder);
      builder.startSpan().end();
      return resultSpan.get();
    }
  }
}
