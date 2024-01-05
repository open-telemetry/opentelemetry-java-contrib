/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.compressor.zstd;

import io.opentelemetry.exporter.internal.compression.Compressor;
import io.opentelemetry.exporter.internal.compression.CompressorProvider;

public final class ZstdCompressorProvider implements CompressorProvider {
  @Override
  public Compressor getInstance() {
    return ZstdCompressor.getInstance();
  }
}
