/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

public class ReadResult {
  /** The consumable data. */
  public final byte[] content;

  public ReadResult(byte[] content) {
    this.content = content;
  }
}
