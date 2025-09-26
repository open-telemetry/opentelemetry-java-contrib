/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.responses;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

public interface ReadableResult<T> extends Closeable {
  /** The consumable data. */
  Collection<T> getContent();

  /** Delete the items provided in {@link #getContent()} */
  void delete() throws IOException;
}
