/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
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
public class TestKeyStore {

  private static final String TYPE = "JKS";

  private final Path path;
  private final String password;

  private TestKeyStore(Path path, String password) {
    this.path = path;
    this.password = password;
  }

  public Path getPath() {
    return path;
  }

  public String getPassword() {
    return password;
  }

  /**
   * Creates a new empty key store
   *
   * @param path key store path
   * @param password key store password
   * @return empty key store
   */
  public static TestKeyStore newKeyStore(Path path, String password) {

    if (Files.exists(path)) {
      throw new IllegalStateException("Keystore already exists " + path);
    }

    try {
      KeyStore keyStore = KeyStore.getInstance(TYPE);
      keyStore.load(null, null);

      TestKeyStore ks = new TestKeyStore(path.toAbsolutePath(), password);
      ks.storeToFile(keyStore);
      return ks;

    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void addTrustedCertificate(X509Certificate certificate) {

    try {
      KeyStore keyStore = loadFromFile();

      keyStore.setCertificateEntry("trustedCertificate", certificate);

      storeToFile(keyStore);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Adds a new public/private key pair and generates a self-signed certificate
   *
   * @return self-signed certificate for created key pair
   */
  public X509Certificate addKeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();

      X509Certificate certificate = createSelfSignedCertificate(keyPair);

      KeyStore keyStore = loadFromFile();

      // for convenience reuse keystore password for key password
      keyStore.setKeyEntry(
          "key", privateKey, password.toCharArray(), new X509Certificate[] {certificate});

      storeToFile(keyStore);

      return certificate;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private KeyStore loadFromFile() throws IOException, GeneralSecurityException {
    KeyStore keyStore = KeyStore.getInstance(TYPE);

    try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ)) {
      keyStore.load(input, password.toCharArray());
    }
    return keyStore;
  }

  private void storeToFile(KeyStore keyStore) throws IOException, GeneralSecurityException {
    try (OutputStream output =
        Files.newOutputStream(
            path,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE)) {
      keyStore.store(output, password.toCharArray());
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
}
