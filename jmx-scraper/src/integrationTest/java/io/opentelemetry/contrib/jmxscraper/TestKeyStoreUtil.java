/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Utility that allows to manage keystores programmatically without using 'keytool' CLI program */
class TestKeyStoreUtil {

  private TestKeyStoreUtil() {}

  /**
   * Creates a keystore with a public/private key pair
   *
   * @param path path to key store
   * @param password key store password
   * @return self-signed certificate of the public/private key pair
   */
  @CanIgnoreReturnValue
  static X509Certificate createKeyStore(Path path, String password) {

    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();

      X509Certificate certificate = createSelfSignedCertificate(keyPair);

      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(null, null);

      // for convenience reuse keystore password for key password
      keyStore.setKeyEntry(
          "key", privateKey, password.toCharArray(), new X509Certificate[] {certificate});

      try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
        keyStore.store(fos, password.toCharArray());
      }

      return certificate;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static X509Certificate createSelfSignedCertificate(KeyPair keyPair) {
    try {
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey privateKey = keyPair.getPrivate();

      Instant now = Instant.now();

      X500Name issuer = new X500Name("CN=Self-Signed Certificate");
      X500Name subject = new X500Name("CN=Self-Signed Certificate");
      BigInteger serial = BigInteger.valueOf(now.toEpochMilli());

      X509v3CertificateBuilder certBuilder =
          new JcaX509v3CertificateBuilder(
              issuer,
              serial,
              Date.from(now.minus(1, ChronoUnit.DAYS)), // 1 day ago
              Date.from(now.plus(1, ChronoUnit.DAYS)), // 1 day from now
              subject,
              publicKey);

      ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
      return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Adds trusted certificate
   *
   * @param path path to key store
   * @param password key store password
   * @param certificate certificate to trust
   */
  public static void addTrustedCertificate(
      Path path, String password, X509Certificate certificate) {

    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");

      try (FileInputStream fis = new FileInputStream(path.toFile())) {
        keyStore.load(fis, password.toCharArray());
      }

      keyStore.setCertificateEntry("trustedCertificate", certificate);

      try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
        keyStore.store(fos, password.toCharArray());
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
