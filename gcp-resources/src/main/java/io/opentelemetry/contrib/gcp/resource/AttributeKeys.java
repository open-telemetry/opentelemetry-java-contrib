/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

/**
 * Contains constants that act as keys for the known attributes for {@link
 * GcpPlatformDetector.SupportedPlatform}s.
 */
final class AttributeKeys {
  private AttributeKeys() {}

  // GCE Attributes
  public static final String GCE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String GCE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;
  public static final String GCE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;
  public static final String GCE_INSTANCE_NAME = AttributeKeys.INSTANCE_NAME;
  public static final String GCE_MACHINE_TYPE = AttributeKeys.MACHINE_TYPE;
  public static final String GCE_INSTANCE_HOSTNAME = "instance_hostname";

  // GKE Attributes
  public static final String GKE_CLUSTER_NAME = "gke_cluster_name";
  public static final String GKE_CLUSTER_LOCATION_TYPE = "gke_cluster_location_type";
  public static final String GKE_CLUSTER_LOCATION = "gke_cluster_location";
  public static final String GKE_HOST_ID = AttributeKeys.INSTANCE_ID;

  // GKE Location Constants
  public static final String GKE_LOCATION_TYPE_ZONE = "ZONE";
  public static final String GKE_LOCATION_TYPE_REGION = "REGION";

  // GAE Attributes
  public static final String GAE_MODULE_NAME = "gae_module_name";
  public static final String GAE_APP_VERSION = "gae_app_version";
  public static final String GAE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;
  public static final String GAE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String GAE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;

  // Google Serverless Compute Attributes
  public static final String SERVERLESS_COMPUTE_NAME = "serverless_compute_name";
  public static final String SERVERLESS_COMPUTE_REVISION = "serverless_compute_revision";
  public static final String SERVERLESS_COMPUTE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String SERVERLESS_COMPUTE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;
  public static final String SERVERLESS_COMPUTE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;

  // Cloud Run Job Specific Attributes
  public static final String GCR_JOB_EXECUTION_KEY = "gcr_job_execution_key";
  public static final String GCR_JOB_TASK_INDEX = "gcr_job_task_index";

  static final String AVAILABILITY_ZONE = "availability_zone";
  static final String CLOUD_REGION = "cloud_region";
  static final String INSTANCE_ID = "instance_id";
  static final String INSTANCE_NAME = "instance_name";
  static final String MACHINE_TYPE = "machine_type";
}
