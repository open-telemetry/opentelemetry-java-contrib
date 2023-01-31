/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@AutoService(ResourceProvider.class)
public final class AppServerServiceNameProvider implements ConditionalResourceProvider {

  private static final Logger logger =
      Logger.getLogger(AppServerServiceNameProvider.class.getName());
  private final ServiceNameDetector detector;

  public AppServerServiceNameProvider() {
    this(CommonAppServersServiceNameDetector.create());
  }

  // Exists for testing
  AppServerServiceNameProvider(ServiceNameDetector detector) {
    this.detector = detector;
  }

  @Override
  public Resource createResource(ConfigProperties config) {
    String serviceName = detectServiceName();
    if (serviceName == null) {
      logger.log(
          WARNING,
          "Service name could not be detected using common application server strategies.");
      return Resource.empty();
    }
    logger.log(INFO, "Auto-detected service name {0}.", serviceName);
    return Resource.create(Attributes.of(SERVICE_NAME, serviceName));
  }

  @Nullable
  private String detectServiceName() {
    try {
      return detector.detect();
    } catch (Exception e) {
      logger.log(
          INFO, "Failed to find a service name using common application server strategies: ", e);
    }
    return null;
  }

  @Override
  public boolean shouldApply(ConfigProperties config, Resource existing) {
    String serviceName = config.getString("otel.service.name");
    if (serviceName != null) {
      logger.log(
          FINE,
          "Skipping AppServerServiceName detection, otel.service.name is already set to {0}",
          serviceName);
      return false;
    }
    Map<String, String> resourceAttributes = config.getMap("otel.resource.attributes");
    if (resourceAttributes.containsKey(SERVICE_NAME.getKey())) {
      logger.log(
          FINE,
          "Skipping AppServerServiceName detection, otel.resource.attributes already contains {0}",
          resourceAttributes.get(SERVICE_NAME.getKey()));
      return false;
    }
    String existingName = existing.getAttribute(SERVICE_NAME);
    if (!"unknown_service:java".equals(existingName)) {
      logger.log(
          FINE,
          "Skipping AppServerServiceName detection, resource already contains {0}",
          existingName);
      return false;
    }
    return true;
  }

  @Override
  public int order() {
    // make it run later than the spring boot resource provider (100)
    return 200;
  }
}
