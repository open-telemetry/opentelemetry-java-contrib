/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Inlines incubating attributes until they are stable, doing this prevents having a direct
 * dependency on incubating artifact which can conflict with another incubating version.
 */
class IncubatingAttributes {

  public static final AttributeKey<String> CLOUD_ACCOUNT_ID =
      AttributeKey.stringKey("cloud.account.id");
  public static final AttributeKey<String> CLOUD_AVAILABILITY_ZONE =
      AttributeKey.stringKey("cloud.availability_zone");
  public static final AttributeKey<String> CLOUD_PLATFORM =
      AttributeKey.stringKey("cloud.platform");
  public static final AttributeKey<String> CLOUD_PROVIDER =
      AttributeKey.stringKey("cloud.provider");
  public static final AttributeKey<String> CLOUD_REGION = AttributeKey.stringKey("cloud.region");

  public static final class CloudPlatformIncubatingValues {

    public static final String GCP_COMPUTE_ENGINE = "gcp_compute_engine";
    public static final String GCP_CLOUD_RUN = "gcp_cloud_run";
    public static final String GCP_KUBERNETES_ENGINE = "gcp_kubernetes_engine";
    public static final String GCP_CLOUD_FUNCTIONS = "gcp_cloud_functions";
    public static final String GCP_APP_ENGINE = "gcp_app_engine";
    public static final String GCP = "gcp";

    private CloudPlatformIncubatingValues() {}
  }

  public static final AttributeKey<String> FAAS_INSTANCE = AttributeKey.stringKey("faas.instance");
  public static final AttributeKey<String> FAAS_NAME = AttributeKey.stringKey("faas.name");
  public static final AttributeKey<String> FAAS_VERSION = AttributeKey.stringKey("faas.version");

  public static final AttributeKey<String> GCP_CLOUD_RUN_JOB_EXECUTION =
      AttributeKey.stringKey("gcp.cloud_run.job.execution");
  public static final AttributeKey<Long> GCP_CLOUD_RUN_JOB_TASK_INDEX =
      AttributeKey.longKey("gcp.cloud_run.job.task_index");

  public static final AttributeKey<String> GCP_GCE_INSTANCE_HOSTNAME =
      AttributeKey.stringKey("gcp.gce.instance.hostname");
  public static final AttributeKey<String> GCP_GCE_INSTANCE_NAME =
      AttributeKey.stringKey("gcp.gce.instance.name");

  public static final AttributeKey<String> HOST_ID = AttributeKey.stringKey("host.id");
  public static final AttributeKey<String> HOST_NAME = AttributeKey.stringKey("host.name");
  public static final AttributeKey<String> HOST_TYPE = AttributeKey.stringKey("host.type");

  public static final AttributeKey<String> K8S_CLUSTER_NAME =
      AttributeKey.stringKey("k8s.cluster.name");

  private IncubatingAttributes() {}
}
