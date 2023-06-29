/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.responses;

public enum ReadableResult {
  SUCCEEDED,
  CLOSED,
  FILE_HAS_EXPIRED,
  PROCESSING_FAILED,
  NO_CONTENT_AVAILABLE
}