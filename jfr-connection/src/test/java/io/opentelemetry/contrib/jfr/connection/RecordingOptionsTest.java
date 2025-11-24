/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.errorprone.annotations.Keep;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RecordingOptionsTest {

  @Keep
  private static Stream<Arguments> testGetName() {
    return Stream.of(
        Arguments.of("test", "test"),
        Arguments.of(" test", "test"),
        Arguments.of(" test ", "test"),
        Arguments.of("", ""),
        Arguments.of(null, ""));
  }

  @ParameterizedTest
  @MethodSource
  void testGetName(String testValue, String expected) {
    RecordingOptions opts = new RecordingOptions.Builder().name(testValue).build();
    assertThat(opts.getName()).isEqualTo(expected);
  }

  @Test
  void testGetNameDefault() {
    String expected = "";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getName()).isEqualTo(expected);
  }

  @Keep
  static Stream<Arguments> testGetMaxAge() {
    return Stream.of(
        Arguments.of("3 ns", "3ns"),
        Arguments.of("3 us", "3us"),
        Arguments.of("3 ms", "3ms"),
        Arguments.of("3 s", "3s"),
        Arguments.of("3 m", "3m"),
        Arguments.of("3 h", "3h"),
        Arguments.of("3 h", "3h"),
        Arguments.of("+3 d", "3d"),
        Arguments.of("3ms", "3ms"),
        Arguments.of("0", "0"),
        Arguments.of("", "0"),
        Arguments.of(null, "0"));
  }

  @ParameterizedTest
  @MethodSource
  void testGetMaxAge(String testValue, String expected) {
    RecordingOptions opts = new RecordingOptions.Builder().maxAge(testValue).build();
    assertThat(opts.getMaxAge()).isEqualTo(expected);
  }

  @Test
  void testGetMaxAgeDefault() {
    String expected = "0";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getMaxAge()).isEqualTo(expected);
  }

  @Keep
  private static Stream<Arguments> testGetMaxAgeNegative() {
    return Stream.of(
        Arguments.of("-3 ms"),
        Arguments.of("3 ps"),
        Arguments.of("3.0 ms"),
        Arguments.of("3-ms"),
        Arguments.of("us"),
        Arguments.of("3_ms"),
        Arguments.of("3 _ms"),
        Arguments.of("3_ ms"),
        Arguments.of("3 _ ms"));
  }

  @ParameterizedTest
  @MethodSource
  void testGetMaxAgeNegative(String badValue) {
    assertThatThrownBy(() -> new RecordingOptions.Builder().maxAge(badValue).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Keep
  private static Stream<Arguments> testGetMaxSize() {
    return Stream.of(
        Arguments.of("12345", "12345"),
        Arguments.of("+54321", "54321"),
        Arguments.of(" 6789", "6789"),
        Arguments.of(" 6789 ", "6789"),
        Arguments.of(" 06789 ", "6789"),
        Arguments.of("0", "0"),
        Arguments.of("", "0"),
        Arguments.of(null, "0"));
  }

  @ParameterizedTest
  @MethodSource
  void testGetMaxSize(String testValue, String expected) {
    RecordingOptions opts = new RecordingOptions.Builder().maxSize(testValue).build();
    assertThat(opts.getMaxSize()).isEqualTo(expected);
  }

  @Test
  void testGetMaxSizeDefault() {
    String expected = "0";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getMaxSize()).isEqualTo(expected);
  }

  @Keep
  private static Stream<Arguments> testGetMaxSizeNegative() {
    return Stream.of(
        Arguments.of("-12345"),
        Arguments.of("5.4321"),
        Arguments.of("BAD"),
        Arguments.of("0xBEEF"));
  }

  @ParameterizedTest
  @MethodSource
  void testGetMaxSizeNegative(String badValue) {
    assertThatThrownBy(() -> new RecordingOptions.Builder().maxSize(badValue).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testGetDumpOnExit() {
    String expected = "true";
    RecordingOptions opts = new RecordingOptions.Builder().dumpOnExit(expected).build();
    assertThat(opts.getDumpOnExit()).isEqualTo(expected);
  }

  @Test
  void testGetDumpOnExitDefault() {
    String expected = "false";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getDumpOnExit()).isEqualTo(expected);
  }

  @Test
  void testGetDumpOnExitBadValue() {
    String expected = "false";
    RecordingOptions opts = new RecordingOptions.Builder().dumpOnExit("BAD_VALUE").build();
    assertThat(opts.getDumpOnExit()).isEqualTo(expected);
  }

  @Keep
  private static Stream<Arguments> testGetDestination() {
    return Stream.of(
        Arguments.of("./destination", "./destination"),
        Arguments.of(" ./destination", "./destination"),
        Arguments.of(" ./destination ", "./destination"),
        Arguments.of("", ""),
        Arguments.of(null, ""));
  }

  @ParameterizedTest
  @MethodSource
  void testGetDestination(String testValue, String expected) {
    RecordingOptions opts = new RecordingOptions.Builder().destination(testValue).build();
    assertThat(opts.getDestination()).isEqualTo(expected);
  }

  @Test
  void testGetDestinationDefault() {
    String expected = "";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getDestination()).isEqualTo(expected);
  }

  @Test
  void testGetDisk() {
    String expected = "true";
    RecordingOptions opts = new RecordingOptions.Builder().disk(expected).build();
    assertThat(opts.getDisk()).isEqualTo(expected);
  }

  @Test
  void testGetDiskDefault() {
    String expected = "false";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getDisk()).isEqualTo(expected);
  }

  @Test
  void testGetDiskBadValue() {
    String expected = "false";
    RecordingOptions opts = new RecordingOptions.Builder().disk("BAD_VALUE").build();
    assertThat(opts.getDisk()).isEqualTo(expected);
  }

  @Keep
  private static Stream<Arguments> testGetDuration() {
    return Stream.of(
        Arguments.of("3 ns", "3ns"),
        Arguments.of("3 us", "3us"),
        Arguments.of("3 ms", "3ms"),
        Arguments.of("3 s", "3s"),
        Arguments.of("3 m", "3m"),
        Arguments.of("3 h", "3h"),
        Arguments.of("3 h", "3h"),
        Arguments.of("+3 d", "3d"),
        Arguments.of("3ms", "3ms"),
        Arguments.of("0", "0"),
        Arguments.of("", "0"),
        Arguments.of(null, "0"));
  }

  @ParameterizedTest
  @MethodSource
  void testGetDuration(String testValue, String expected) {
    RecordingOptions opts = new RecordingOptions.Builder().duration(testValue).build();
    assertThat(opts.getDuration()).isEqualTo(expected);
  }

  @Test
  void testGetDurationDefault() {
    String expected = "0";
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getDuration()).isEqualTo(expected);
  }

  @Keep
  private static Stream<Arguments> testGetDurationNegative() {
    return Stream.of(
        Arguments.of("-3 ms"),
        Arguments.of("3 ps"),
        Arguments.of("3.0 ms"),
        Arguments.of("3-ms"),
        Arguments.of("us"),
        Arguments.of("3_ms"),
        Arguments.of("3 _ms"),
        Arguments.of("3_ ms"),
        Arguments.of("3 _ ms"));
  }

  @ParameterizedTest
  @MethodSource
  void testGetDurationNegative(String badValue) {
    assertThatThrownBy(() -> new RecordingOptions.Builder().duration(badValue).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testGetRecordingOptions() {
    Map<String, String> expected = new HashMap<>();
    expected.put("name", "test");
    expected.put("maxAge", "3m");
    expected.put("maxSize", "1048576");
    expected.put("dumpOnExit", "true");
    expected.put("destination", "test.jfr");
    expected.put("disk", "true");
    expected.put("duration", "120s");
    RecordingOptions opts =
        new RecordingOptions.Builder()
            .name("test")
            .maxAge("3 m")
            .maxSize("1048576")
            .dumpOnExit("true")
            .destination("test.jfr")
            .disk("true")
            .duration("120 s")
            .build();
    assertThat(opts.getRecordingOptions()).isEqualTo(expected);
  }

  @Test
  void testGetRecordingOptionsDefaults() {
    Map<String, String> expected = new HashMap<>();
    // Due to a bug, some JVMs default "disk=true". So include "disk=false" (the documented default)
    // to insure consistent behaviour.
    expected.put("disk", "false");
    RecordingOptions opts = new RecordingOptions.Builder().build();
    assertThat(opts.getRecordingOptions()).isEqualTo(expected);
  }
}
