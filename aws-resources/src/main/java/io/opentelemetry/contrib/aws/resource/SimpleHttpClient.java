/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** A simple HTTP client based on OkHttp. Not meant for high throughput. */
final class SimpleHttpClient {

  private static final Logger logger = Logger.getLogger(SimpleHttpClient.class.getName());

  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  @Nullable
  private static SSLSocketFactory getSslSocketFactoryForCertPath(@Nullable String certPath) {
    if (Objects.isNull(certPath)) {
      return null;
    }

    KeyStore keyStore = getKeystoreForTrustedCert(certPath);
    X509TrustManager trustManager = buildTrustManager(keyStore);
    return buildSslSocketFactory(trustManager);
  }

  private static HttpURLConnection setupUrlConnection(
      String urlStr, String httpMethod, Map<String, String> headers) throws Exception {
    try {
      HttpURLConnection urlConnection = (HttpURLConnection) new URL(urlStr).openConnection();
      urlConnection.setRequestMethod(httpMethod);
      headers.forEach(urlConnection::setRequestProperty);
      urlConnection.setConnectTimeout((int) TIMEOUT.toMillis());
      urlConnection.setReadTimeout((int) TIMEOUT.toMillis());
      urlConnection.setDoInput(true);
      urlConnection.setDoOutput(false);
      return urlConnection;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot open connection to " + urlStr, e);
      throw e;
    }
  }

  /** Fetch a string from a remote server. */
  public String fetchString(
      String httpMethod, String urlStr, Map<String, String> headers, @Nullable String certPath) {

    try {
      HttpURLConnection httpUrlConnection = setupUrlConnection(urlStr, httpMethod, headers);
      if (urlStr.startsWith("https")) {
        HttpsURLConnection urlConnection = (HttpsURLConnection) httpUrlConnection;
        SSLSocketFactory sslSocketFactory = getSslSocketFactoryForCertPath(certPath);
        urlConnection.setSSLSocketFactory(sslSocketFactory);
      }

      int responseCode = httpUrlConnection.getResponseCode();
      String responseBody = convert(httpUrlConnection.getInputStream());

      if (responseCode != 200) {
        logger.log(
            Level.FINE,
            "Error response from " + urlStr + " code (" + responseCode + ") text " + responseBody);
        return "";
      }
      return responseBody;
    } catch (Exception e) {
      logger.log(Level.FINE, "SimpleHttpClient fetch string failed.", e);
    }

    return "";
  }

  @Nullable
  private static X509TrustManager buildTrustManager(@Nullable KeyStore keyStore) {
    if (keyStore == null) {
      return null;
    }
    try {
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
      tmf.init(keyStore);
      return (X509TrustManager) tmf.getTrustManagers()[0];
    } catch (Exception e) {
      logger.log(Level.WARNING, "Build SslSocketFactory for K8s restful client exception.", e);
      return null;
    }
  }

  @Nullable
  private static SSLSocketFactory buildSslSocketFactory(@Nullable TrustManager trustManager) {
    if (trustManager == null) {
      return null;
    }
    try {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[] {trustManager}, null);
      return context.getSocketFactory();

    } catch (Exception e) {
      logger.log(Level.WARNING, "Build SslSocketFactory for K8s restful client exception.", e);
    }
    return null;
  }

  @Nullable
  private static KeyStore getKeystoreForTrustedCert(String certPath) {
    try (FileInputStream fis = new FileInputStream(certPath)) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

      Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(fis);

      int i = 0;
      for (Certificate certificate : certificates) {
        trustStore.setCertificateEntry("cert_" + i, certificate);
        i++;
      }
      return trustStore;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Cannot load KeyStore from " + certPath);
      return null;
    }
  }

  public static String convert(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    return result.toString(StandardCharsets.UTF_8.name());
  }
}
