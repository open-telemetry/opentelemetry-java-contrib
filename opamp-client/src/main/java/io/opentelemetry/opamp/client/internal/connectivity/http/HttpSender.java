/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public interface HttpSender {

  CompletableFuture<Response> send(BodyWriter writer, int contentLength);

  interface BodyWriter {
    void writeTo(OutputStream outputStream) throws IOException;
  }

  interface Response extends Closeable {
    int statusCode();

    String statusMessage();

    InputStream bodyInputStream();

    String getHeader(String name);
  }
}
