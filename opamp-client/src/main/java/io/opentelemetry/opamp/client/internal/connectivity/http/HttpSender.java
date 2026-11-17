/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sends HTTP requests to an OpAMP server.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface HttpSender {

  Response send(BodyWriter writer, int contentLength) throws IOException;

  /** Writes the request body to an output stream. */
  interface BodyWriter {
    void writeTo(OutputStream outputStream) throws IOException;
  }

  /** HTTP response returned by {@link HttpSender}. */
  interface Response extends Closeable {
    int statusCode();

    String statusMessage();

    InputStream bodyInputStream();

    String getHeader(String name);
  }
}
