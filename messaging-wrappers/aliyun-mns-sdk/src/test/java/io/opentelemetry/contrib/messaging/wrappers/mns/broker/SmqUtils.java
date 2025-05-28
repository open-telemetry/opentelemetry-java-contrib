/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns.broker;

import com.aliyun.mns.model.ErrorMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public final class SmqUtils {

  public static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byteArrayOutputStream.write(buffer, 0, bytesRead);
      }
      return byteArrayOutputStream.toByteArray();
    }
  }

  public static String calculateMessageBodyMD5(String messageBody) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashBytes = md.digest(messageBody.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(hashBytes).toUpperCase(Locale.ROOT);
  }

  public static String generateRandomMessageId() {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase(Locale.ROOT);
  }

  public static String generateRandomRequestId() {
    return UUID.randomUUID()
        .toString()
        .replaceAll("-", "")
        .substring(0, 24)
        .toUpperCase(Locale.ROOT);
  }

  public static ErrorMessage createErrorMessage(String requestId) {
    ErrorMessage errorMessage = new ErrorMessage();
    errorMessage.Code = "MessageNotExist";
    errorMessage.Message = "Message not exist.";
    errorMessage.RequestId = requestId;
    errorMessage.HostId = "http://test.mns.cn-hangzhou.aliyuncs.com";
    return errorMessage;
  }

  public static String generateRandomBase64String(int length) {
    byte[] randomBytes = new byte[length];
    new Random().nextBytes(randomBytes);

    return Base64.getEncoder().encodeToString(randomBytes);
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format(Locale.ROOT, "%02X", b));
    }
    return sb.toString();
  }

  private SmqUtils() {}
}
