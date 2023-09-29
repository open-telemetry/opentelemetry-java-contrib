/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/** A simple HTTP client based on JDK HttpURLConnection. Not meant for high throughput. */
final class JdkHttpClient {

  private static final Logger logger = Logger.getLogger(JdkHttpClient.class.getName());

  private static final int TIMEOUT = 2000;

  /** Fetch a string from a remote server. */
  public String fetchString(
      String httpMethod, String urlStr, Map<String, String> headers, @Nullable String certPath) {

    try {
      // create URL from string
      URL url = new URL(urlStr);
      // create connection
      URLConnection connection = url.openConnection();
      // https
      if (connection instanceof HttpsURLConnection) {
        // cast
        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        // check CA cert path is available
        if (certPath != null) {
          // create trust manager
          X509TrustManager trustManager = SslSocketFactoryBuilder.createTrustManager(certPath);
          // socket factory
          SSLSocketFactory socketFactory =
              SslSocketFactoryBuilder.createSocketFactory(trustManager);
          if (socketFactory != null) {
            // update connection
            httpsConnection.setSSLSocketFactory(socketFactory);
          }
        }
        // process request
        return processRequest(httpsConnection, httpMethod, urlStr, headers);
      }
      // http
      if (connection instanceof HttpURLConnection) {
        // cast
        HttpURLConnection httpConnection = (HttpURLConnection) connection;
        // process request
        return processRequest(httpConnection, httpMethod, urlStr, headers);
      }
      // not http
      logger.log(Level.FINE, "JdkHttpClient only HTTP/HTTPS connections are supported.");
    } catch (MalformedURLException e) {
      logger.log(Level.FINE, "JdkHttpClient invalid URL.", e);
    } catch (IOException e) {
      logger.log(Level.FINE, "JdkHttpClient fetch string failed.", e);
    }
    return "";
  }

  private static String processRequest(
      HttpURLConnection httpConnection,
      String httpMethod,
      String urlStr,
      Map<String, String> headers)
      throws IOException {
    // set method
    httpConnection.setRequestMethod(httpMethod);
    // set headers
    headers.forEach(httpConnection::setRequestProperty);
    // timeouts
    httpConnection.setConnectTimeout(TIMEOUT);
    httpConnection.setReadTimeout(TIMEOUT);
    // connect
    httpConnection.connect();
    try {
      // status code
      int responseCode = httpConnection.getResponseCode();
      if (responseCode != 200) {
        logger.log(
            Level.FINE,
            "Error response from "
                + urlStr
                + " code ("
                + responseCode
                + ") text "
                + httpConnection.getResponseMessage());
        return "";
      }
      // read response
      try (InputStream inputStream = httpConnection.getInputStream()) {
        // store read data in byte array
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
          // read all bytes
          int b;
          while ((b = inputStream.read()) != -1) {
            outputStream.write(b);
          }
          // result
          return outputStream.toString("UTF-8");
        }
      }
    } finally {
      // disconnect, no need for persistent connections
      httpConnection.disconnect();
    }
  }
}
