/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

public final class ReadResult {
  /** The consumable data. */
  public final byte[] content;

  /**
   * The total amount of data read from the stream. This number can be greater than the content
   * length as it also takes into account any delimiters size.
   */
  public final int totalReadLength;

  public ReadResult(byte[] content, int totalReadLength) {
    this.content = content;
    this.totalReadLength = totalReadLength;
  }
}
