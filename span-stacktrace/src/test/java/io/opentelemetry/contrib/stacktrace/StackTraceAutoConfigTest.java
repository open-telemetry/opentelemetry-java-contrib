/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class StackTraceAutoConfigTest {

  @Test
  void defaultConfig() {
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(Collections.emptyMap());
    assertThat(StackTraceAutoConfig.getMinDuration(config)).isEqualTo(5000000L);
    Predicate<ReadableSpan> filterPredicate = StackTraceAutoConfig.getFilterPredicate(config);
    assertThat(filterPredicate).isNotNull();
  }

  @Test
  void minDurationValue() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("otel.java.experimental.span-stacktrace.min.duration", "42ms");
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    assertThat(StackTraceAutoConfig.getMinDuration(config)).isEqualTo(42000000L);
  }

  @Test
  void negativeMinDuration() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("otel.java.experimental.span-stacktrace.min.duration", "-1");
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    assertThat(StackTraceAutoConfig.getMinDuration(config)).isNegative();
  }

  @Test
  void customFilter() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("otel.java.experimental.span-stacktrace.filter", MyFilter.class.getName());
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    Predicate<ReadableSpan> filterPredicate = StackTraceAutoConfig.getFilterPredicate(config);
    assertThat(filterPredicate).isInstanceOf(MyFilter.class);

    // default does not filter, so any negative value means we use the test filter
    assertThat(filterPredicate.test(null)).isFalse();
  }

  public static class MyFilter implements Predicate<ReadableSpan> {
    @Override
    public boolean test(ReadableSpan readableSpan) {
      return false;
    }
  }

  @Test
  void brokenFilter_classVisibility() {
    testBrokenFilter(BrokenFilter.class.getName());
  }

  @Test
  void brokenFilter_type() {
    testBrokenFilter(Object.class.getName());
  }

  @Test
  void brokenFilter_missingType() {
    testBrokenFilter("missing.class.name");
  }

  private static void testBrokenFilter(String filterName) {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("otel.java.experimental.span-stacktrace.filter", filterName);
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    Predicate<ReadableSpan> filterPredicate = StackTraceAutoConfig.getFilterPredicate(config);
    assertThat(filterPredicate).isNotNull();
    assertThat(filterPredicate.test(null)).isTrue();
  }

  private static class BrokenFilter extends MyFilter {}
}
