/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

class GcpPlatformDetector {
  public static final GcpPlatformDetector DEFAULT_INSTANCE = new GcpPlatformDetector();

  private final GcpMetadataConfig metadataConfig;
  private final EnvironmentVariables environmentVariables;

  // for testing only
  GcpPlatformDetector(GcpMetadataConfig metadataConfig, EnvironmentVariables environmentVariables) {
    this.metadataConfig = metadataConfig;
    this.environmentVariables = environmentVariables;
  }

  private GcpPlatformDetector() {
    this.metadataConfig = GcpMetadataConfig.DEFAULT_INSTANCE;
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
  }

  /**
   * Detects the GCP platform on which the application is running.
   *
   * @return the specific GCP platform on which the application is running.
   */
  public DetectedPlatform detectPlatform() {
    return generateDetectedPlatform(detectSupportedPlatform());
  }

  private SupportedPlatform detectSupportedPlatform() {
    if (!isRunningOnGcp()) {
      return SupportedPlatform.UNKNOWN_PLATFORM;
    }
    // Note: Order of detection matters here
    if (environmentVariables.get("KUBERNETES_SERVICE_HOST") != null) {
      return SupportedPlatform.GOOGLE_KUBERNETES_ENGINE;
    } else if (environmentVariables.get("K_CONFIGURATION") != null
        && environmentVariables.get("FUNCTION_TARGET") == null) {
      return SupportedPlatform.GOOGLE_CLOUD_RUN;
    } else if (environmentVariables.get("FUNCTION_TARGET") != null) {
      return SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS;
    } else if (environmentVariables.get("CLOUD_RUN_JOB") != null) {
      return SupportedPlatform.GOOGLE_CLOUD_RUN_JOB;
    } else if (environmentVariables.get("GAE_SERVICE") != null) {
      return SupportedPlatform.GOOGLE_APP_ENGINE;
    }
    return SupportedPlatform.GOOGLE_COMPUTE_ENGINE; // default to GCE
  }

  private boolean isRunningOnGcp() {
    return metadataConfig.getProjectId() != null && !metadataConfig.getProjectId().isEmpty();
  }

  private DetectedPlatform generateDetectedPlatform(SupportedPlatform platform) {
    DetectedPlatform detectedPlatform;
    switch (platform) {
      case GOOGLE_KUBERNETES_ENGINE:
        detectedPlatform = new GoogleKubernetesEngine(metadataConfig);
        break;
      case GOOGLE_CLOUD_RUN:
        detectedPlatform = new GoogleCloudRun(environmentVariables, metadataConfig);
        break;
      case GOOGLE_CLOUD_FUNCTIONS:
        detectedPlatform = new GoogleCloudFunction(environmentVariables, metadataConfig);
        break;
      case GOOGLE_CLOUD_RUN_JOB:
        detectedPlatform = new GoogleCloudRunJob(environmentVariables, metadataConfig);
        break;
      case GOOGLE_APP_ENGINE:
        detectedPlatform = new GoogleAppEngine(environmentVariables, metadataConfig);
        break;
      case GOOGLE_COMPUTE_ENGINE:
        detectedPlatform = new GoogleComputeEngine(metadataConfig);
        break;
      default:
        detectedPlatform = new UnknownPlatform();
    }
    return detectedPlatform;
  }

  /**
   * SupportedPlatform represents the GCP platforms that can currently be detected by the
   * resource-detector.
   */
  enum SupportedPlatform {
    /** Represents the Google Compute Engine platform. */
    GOOGLE_COMPUTE_ENGINE,
    /** Represents the Google Kubernetes Engine platform. */
    GOOGLE_KUBERNETES_ENGINE,
    /** Represents the Google App Engine platform. Could either be flex or standard. */
    GOOGLE_APP_ENGINE,
    /** Represents the Google Cloud Run platform (Service). */
    GOOGLE_CLOUD_RUN,
    /** Represents the Google Cloud Run platform (Jobs). */
    GOOGLE_CLOUD_RUN_JOB,
    /** Represents the Google Cloud Functions platform. */
    GOOGLE_CLOUD_FUNCTIONS,
    /** Represents the case when the application is not running on GCP. */
    UNKNOWN_PLATFORM,
  }
}
