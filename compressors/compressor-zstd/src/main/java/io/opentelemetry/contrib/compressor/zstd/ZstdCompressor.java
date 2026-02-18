/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.compressor.zstd;

import com.github.luben.zstd.ZstdOutputStream;
import io.opentelemetry.sdk.common.export.Compressor;
import java.io.IOException;
import java.io.OutputStream;

public final class ZstdCompressor implements Compressor {

  public ZstdCompressor() {}

  @Override
  public String getEncoding() {
    return "zstd";
  }

  @Override
  public OutputStream compress(OutputStream outputStream) throws IOException {
    return new ZstdOutputStream(outputStream);
  }
}
