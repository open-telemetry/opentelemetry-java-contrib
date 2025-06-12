/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OkHttpSender implements HttpSender {
  private final OkHttpClient client;
  private final String url;

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
    this.client = client;
  }

  @Override
  public CompletableFuture<Response> send(BodyWriter writer, int contentLength) {
    CompletableFuture<Response> future = new CompletableFuture<>();
    okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);
    builder.addHeader("Content-Type", CONTENT_TYPE);

    RequestBody body = new RawRequestBody(writer, contentLength, MEDIA_TYPE);
    builder.post(body);

    client
        .newCall(builder.build())
        .enqueue(
            new Callback() {
              @Override
              public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) {
                if (response.isSuccessful() && response.body() != null) {
                  future.complete(new OkHttpResponse(response));
                } else {
                  future.completeExceptionally(
                      new HttpErrorException(response.code(), response.message()));
                }
              }

              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
              }
            });

    return future;
  }

  private static class OkHttpResponse implements Response {
    private final okhttp3.Response response;

    private OkHttpResponse(okhttp3.Response response) {
      if (response.body() == null) {
        throw new IllegalStateException("null response body not expected");
      }
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
    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
      writer.writeTo(bufferedSink.outputStream());
    }
  }
}
