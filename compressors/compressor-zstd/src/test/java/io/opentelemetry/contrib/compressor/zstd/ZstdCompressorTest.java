/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.compressor.zstd;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.luben.zstd.ZstdInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ZstdCompressorTest {

  @Test
  void roundTrip() throws IOException {
    String content = "hello world";

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream os = ZstdCompressor.getInstance().compress(baos);
    os.write(content.getBytes(StandardCharsets.UTF_8));
    os.close();

    byte[] decompressed = new byte[content.length()];
    InputStream is = new ZstdInputStream(new ByteArrayInputStream(baos.toByteArray()));
    is.read(decompressed);
    is.close();

    assertThat(new String(decompressed, StandardCharsets.UTF_8)).isEqualTo(content);
  }
}
