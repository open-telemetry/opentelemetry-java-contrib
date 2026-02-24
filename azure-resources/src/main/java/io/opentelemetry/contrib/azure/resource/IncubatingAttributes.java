/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import io.opentelemetry.api.common.AttributeKey;

// copied from opentelemetry-semconv-incubating
final class IncubatingAttributes {

  // cloud attributes
  public static final AttributeKey<String> CLOUD_PLATFORM =
      AttributeKey.stringKey("cloud.platform");
  public static final AttributeKey<String> CLOUD_PROVIDER =
      AttributeKey.stringKey("cloud.provider");
  public static final AttributeKey<String> CLOUD_REGION = AttributeKey.stringKey("cloud.region");
  public static final AttributeKey<String> CLOUD_RESOURCE_ID =
      AttributeKey.stringKey("cloud.resource_id");

  public static final class CloudPlatformIncubatingValues {
    public static final String AZURE_VM = "azure.vm";
    public static final String AZURE_AKS = "azure.aks";
    public static final String AZURE_FUNCTIONS = "azure.functions";
    public static final String AZURE_APP_SERVICE = "azure.app_service";

    private CloudPlatformIncubatingValues() {}
  }

  public static final class CloudProviderIncubatingValues {
    public static final String AZURE = "azure";

    private CloudProviderIncubatingValues() {}
  }

  // deployment attributes
  public static final AttributeKey<String> DEPLOYMENT_ENVIRONMENT_NAME =
      AttributeKey.stringKey("deployment.environment.name");

  // host attributes
  public static final AttributeKey<String> HOST_ID = AttributeKey.stringKey("host.id");
  public static final AttributeKey<String> HOST_NAME = AttributeKey.stringKey("host.name");
  public static final AttributeKey<String> HOST_TYPE = AttributeKey.stringKey("host.type");

  // faas attributes
  public static final AttributeKey<String> FAAS_INSTANCE = AttributeKey.stringKey("faas.instance");
  public static final AttributeKey<Long> FAAS_MAX_MEMORY = AttributeKey.longKey("faas.max_memory");
  public static final AttributeKey<String> FAAS_NAME = AttributeKey.stringKey("faas.name");
  public static final AttributeKey<String> FAAS_VERSION = AttributeKey.stringKey("faas.version");

  // host attributes
  static final AttributeKey<String> K8S_CLUSTER_NAME = AttributeKey.stringKey("k8s.cluster.name");

  // OS attributes
  public static final AttributeKey<String> OS_TYPE = AttributeKey.stringKey("os.type");
  public static final AttributeKey<String> OS_VERSION = AttributeKey.stringKey("os.version");

  private IncubatingAttributes() {}
}
