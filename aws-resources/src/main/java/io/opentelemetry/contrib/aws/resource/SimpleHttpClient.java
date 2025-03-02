/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** A simple HTTP client based on OkHttp. Not meant for high throughput. */
final class SimpleHttpClient {

  private static final Logger logger = Logger.getLogger(SimpleHttpClient.class.getName());

  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  private static final RequestBody EMPTY_BODY = RequestBody.create(new byte[0]);

  /** Fetch a string from a remote server. */
  public String fetchString(
      String httpMethod, String urlStr, Map<String, String> headers, @Nullable String certPath) {

    OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder()
            .callTimeout(TIMEOUT)
            .connectTimeout(TIMEOUT)
            .readTimeout(TIMEOUT);

    if (urlStr.startsWith("https") && certPath != null) {
      X509TrustManager trustManager = SslSocketFactoryBuilder.createTrustManager(certPath);
      SSLSocketFactory socketFactory = SslSocketFactoryBuilder.createSocketFactory(trustManager);
      if (socketFactory != null) {
        clientBuilder.sslSocketFactory(socketFactory, trustManager);
      }
    }

    OkHttpClient client = clientBuilder.build();

    // AWS incorrectly uses PUT despite having no request body, OkHttp will only allow us to send
    // GET with null body or PUT with empty string body
    RequestBody requestBody = null;
    if (httpMethod.equals("PUT")) {
      requestBody = EMPTY_BODY;
    }
    Request.Builder requestBuilder =
        new Request.Builder().url(urlStr).method(httpMethod, requestBody);

    headers.forEach(requestBuilder::addHeader);

    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      int responseCode = response.code();
      if (responseCode != 200) {
        logger.log(
            Level.FINE,
            "Error response from "
                + urlStr
                + " code ("
                + responseCode
                + ") text "
                + response.message());
        return "";
      }
      ResponseBody body = response.body();
      return body != null ? body.string() : "";
    } catch (IOException e) {
      logger.log(Level.FINE, "SimpleHttpClient fetch string failed.", e);
    }

    return "";
  }
}
