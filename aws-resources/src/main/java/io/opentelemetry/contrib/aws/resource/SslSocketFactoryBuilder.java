package io.opentelemetry.contrib.aws.resource;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

class SslSocketFactoryBuilder {

  private static final Logger logger = Logger.getLogger(SslSocketFactoryBuilder.class.getName());

  private SslSocketFactoryBuilder() {}

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

  @Nullable
  static X509TrustManager createTrustManager(@Nullable String certPath) {
    if (certPath == null) {
      return null;
    }
    try {
      // create keystore
      KeyStore keyStore = getKeystoreForTrustedCert(certPath);
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
  static SSLSocketFactory createSocketFactory(@Nullable TrustManager trustManager) {
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
}
