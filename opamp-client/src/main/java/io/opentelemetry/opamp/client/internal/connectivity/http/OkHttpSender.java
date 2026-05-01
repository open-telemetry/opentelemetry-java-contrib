/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import io.opentelemetry.api.internal.InstrumentationUtil;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;

public final class OkHttpSender implements HttpSender {
  private final OkHttpClient client;
  private final String url;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  public static OkHttpSender create(String url) {
    return create(url, new OkHttpClient());
  }

  public static OkHttpSender create(String url, OkHttpClient client) {
    return new OkHttpSender(url, client);
  }

  private static final String CONTENT_TYPE = "application/x-protobuf";
  private static final MediaType MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);

  private OkHttpSender(String url, OkHttpClient client) {
    this.url = url;
    this.client = client.newBuilder().callTimeout(REQUEST_TIMEOUT).build();
  }

  @Override
  public Response send(BodyWriter writer, int contentLength) throws IOException {
    Request.Builder builder = new Request.Builder().url(url);
    builder.addHeader("Content-Type", CONTENT_TYPE);

    RequestBody body = new RawRequestBody(writer, contentLength, MEDIA_TYPE);
    builder.post(body);

    AtomicReference<Response> responseRef = new AtomicReference<>();
    AtomicReference<IOException> errorRef = new AtomicReference<>();
    // By suppressing instrumentations, we prevent automatic instrumentations for the okhttp request
    // that polls the opamp server.
    InstrumentationUtil.suppressInstrumentation(
        () -> {
          try {
            responseRef.set(doSendRequest(builder.build()));
          } catch (IOException e) {
            errorRef.set(e);
          }
        });
    if (errorRef.get() != null) {
      throw errorRef.get();
    }
    return Objects.requireNonNull(responseRef.get());
  }

  private Response doSendRequest(Request request) throws IOException {
    okhttp3.Response response = client.newCall(request).execute();
    return new OkHttpResponse(response);
  }

  private static class OkHttpResponse implements Response {
    private final okhttp3.Response response;

    private OkHttpResponse(okhttp3.Response response) {
      this.response = response;
    }

    @Override
    public int statusCode() {
      return response.code();
    }

    @Override
    public String statusMessage() {
      return response.message();
    }

    @Override
    public InputStream bodyInputStream() {
      return response.body().byteStream();
    }

    @Override
    public String getHeader(String name) {
      return response.headers().get(name);
    }

    @Override
    public void close() {
      response.close();
    }
  }

  private static class RawRequestBody extends RequestBody {
    private final BodyWriter writer;
    private final int contentLength;
    private final MediaType contentType;

    private RawRequestBody(BodyWriter writer, int contentLength, MediaType contentType) {
      this.writer = writer;
      this.contentLength = contentLength;
      this.contentType = contentType;
    }

    @Nullable
    @Override
    public MediaType contentType() {
      return contentType;
    }

    @Override
    public long contentLength() {
      return contentLength;
    }

    @Override
    public void writeTo(BufferedSink bufferedSink) throws IOException {
      writer.writeTo(bufferedSink.outputStream());
    }
  }
}
