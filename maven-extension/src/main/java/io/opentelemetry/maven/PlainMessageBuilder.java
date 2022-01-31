/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import com.google.errorprone.annotations.FormatMethod;
import org.apache.maven.shared.utils.logging.MessageBuilder;

/**
 * Message builder implementation that just ignores styling, for Maven version earlier than 3.4.0.
 *
 * <p>Copy of org.apache.maven.shared.utils.logging.PlainMessageBuilder to get a visible constructor
 */
class PlainMessageBuilder implements MessageBuilder {
  private final StringBuilder buffer;

  PlainMessageBuilder() {
    buffer = new StringBuilder();
  }

  @Override
  public PlainMessageBuilder debug(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder info(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder warning(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder error(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder success(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder failure(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder strong(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder mojo(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder project(Object message) {
    return a(message);
  }

  @Override
  public PlainMessageBuilder a(char[] value, int offset, int len) {
    buffer.append(value, offset, len);
    return this;
  }

  @Override
  public PlainMessageBuilder a(char[] value) {
    buffer.append(value);
    return this;
  }

  @Override
  public PlainMessageBuilder a(CharSequence value, int start, int end) {
    buffer.append(value, start, end);
    return this;
  }

  @Override
  public PlainMessageBuilder a(CharSequence value) {
    buffer.append(value);
    return this;
  }

  @Override
  public PlainMessageBuilder a(Object value) {
    buffer.append(value);
    return this;
  }

  @Override
  public PlainMessageBuilder newline() {
    buffer.append(System.getProperty("line.separator"));
    return this;
  }

  @FormatMethod
  @Override
  public PlainMessageBuilder format(String pattern, Object... args) {
    buffer.append(String.format(pattern, args));
    return this;
  }

  @Override
  public String toString() {
    return buffer.toString();
  }
}
