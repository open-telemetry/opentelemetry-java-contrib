/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface HttpSender {

  CompletableFuture<Response> send(Consumer<OutputStream> writer, int contentLength);

  interface Response extends Closeable {
    int statusCode();

    String statusMessage();

    InputStream bodyInputStream();

    String getHeader(String name);
  }
}
