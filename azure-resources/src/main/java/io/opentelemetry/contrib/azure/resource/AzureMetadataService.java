/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AzureMetadataService {
  static final JsonFactory JSON_FACTORY = new JsonFactory();
  private static final URL METADATA_URL;

  static {
    try {
      METADATA_URL = new URL("http://169.254.169.254/metadata/instance?api-version=2021-02-01");
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  private AzureMetadataService() {}

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private static final Logger logger = Logger.getLogger(AzureMetadataService.class.getName());

  static Supplier<Optional<String>> defaultClient() {
    return () -> fetchMetadata(METADATA_URL);
  }

  // visible for testing
  static Optional<String> fetchMetadata(URL url) {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .callTimeout(TIMEOUT)
            .connectTimeout(TIMEOUT)
            .readTimeout(TIMEOUT)
            .build();

    Request request = new Request.Builder().url(url).get().addHeader("Metadata", "true").build();

    try (Response response = client.newCall(request).execute()) {
      int responseCode = response.code();
      if (responseCode != 200) {
        logger.log(
            Level.FINE,
            "Error response from "
                + url
                + " code ("
                + responseCode
                + ") text "
                + response.message());
        return Optional.empty();
      }

      return Optional.of(Objects.requireNonNull(response.body()).string());
    } catch (IOException e) {
      logger.log(Level.FINE, "Failed to fetch Azure VM metadata", e);
      return Optional.empty();
    }
  }
}
