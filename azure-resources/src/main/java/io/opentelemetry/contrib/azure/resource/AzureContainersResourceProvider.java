/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.azure.resource;

import static io.opentelemetry.contrib.azure.resource.IncubatingAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.HashMap;
import java.util.Map;

public final class AzureContainersResourceProvider extends CloudResourceProvider {

  static final String CONTAINER_APP_NAME = "CONTAINER_APP_NAME";

  private static final String CONTAINER_APP_REPLICA_NAME = "CONTAINER_APP_REPLICA_NAME";
  private static final String CONTAINER_APP_REVISION = "CONTAINER_APP_REVISION";

  private static final Map<AttributeKey<String>, String> ENV_VAR_MAPPING = new HashMap<>();

  static {
    ENV_VAR_MAPPING.put(SERVICE_NAME, CONTAINER_APP_NAME);
    ENV_VAR_MAPPING.put(SERVICE_INSTANCE_ID, CONTAINER_APP_REPLICA_NAME);
    ENV_VAR_MAPPING.put(SERVICE_VERSION, CONTAINER_APP_REVISION);
  }

  private final Map<String, String> env;

  // SPI
  public AzureContainersResourceProvider() {
    this(System.getenv());
  }

  // Visible for testing
  AzureContainersResourceProvider(Map<String, String> env) {
    this.env = env;
  }

  @Override
  public Resource createResource() {
    return Resource.create(getAttributes());
  }

  public Attributes getAttributes() {
    AzureEnvVarPlatform detect = AzureEnvVarPlatform.detect(env);
    if (detect != AzureEnvVarPlatform.CONTAINER_APP) {
      return Attributes.empty();
    }

    AttributesBuilder builder =
        AzureVmResourceProvider.azureAttributeBuilder("azure_container_apps");

    AzureEnvVarPlatform.addAttributesFromEnv(ENV_VAR_MAPPING, env, builder);

    return builder.build();
  }
}
