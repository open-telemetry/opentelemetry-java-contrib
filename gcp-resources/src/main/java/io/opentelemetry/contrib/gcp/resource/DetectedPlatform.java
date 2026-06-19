/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.util.Map;

/** Represents a GCP specific platform on which a cloud application can run. */
interface DetectedPlatform {
  /**
   * Method to retrieve the underlying compute platform on which application is running.
   *
   * @return the {@link GcpPlatformDetector.SupportedPlatform} representing the Google Cloud
   *     platform on which application is running.
   */
  GcpPlatformDetector.SupportedPlatform getSupportedPlatform();

  /**
   * Method to retrieve the GCP Project ID in which the GCP specific platform exists. Every valid
   * platform must have a GCP Project ID associated with it.
   *
   * @return the Google Cloud project ID.
   */
  String getProjectId();

  /**
   * Method to retrieve the attributes associated with the compute platform on which the application
   * is running as key-value pairs. The valid keys to query on this {@link Map} are specified in the
   * {@link AttributeKeys}.
   *
   * @return a {@link Map} of attributes specific to the underlying compute platform.
   */
  Map<String, String> getAttributes();
}
