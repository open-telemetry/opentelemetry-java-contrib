/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * This class is a BeforeAgentListener that hooks into the agent lifecycle to determine
 * if the otel.service.name has been set. It will log a warning to help the user better
 * understand how to set their service name.
 *
 * The serviceNameNotConfigured method is also used by some resource providers to
 * determine if they should apply (eg. don't change the service name if already set).
 */
@AutoService(BeforeAgentListener.class)
public class ServiceNameChecker implements BeforeAgentListener {
  private static final Logger logger = Logger.getLogger(ServiceNameChecker.class.getName());

  private final Consumer<String> logWarn;

  @SuppressWarnings("unused")
  public ServiceNameChecker() {
    this(logger::warning);
  }

  // visible for tests
  ServiceNameChecker(Consumer<String> logWarn) {
    this.logWarn = logWarn;
  }

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = autoConfiguredOpenTelemetrySdk.getConfig();
    Resource resource = autoConfiguredOpenTelemetrySdk.getResource();
    if (serviceNameNotConfigured(config, resource)) {
      logWarn.accept(
          "Resource attribute 'otel.service.name' is not set: your service is unnamed and will be difficult to identify."
              + " Please Set your service name using the 'OTEL_RESOURCE_ATTRIBUTES' environment variable"
              + " or the 'otel.resource.attributes' system property."
              + " E.g. 'export OTEL_RESOURCE_ATTRIBUTES=\"service.name=<YOUR_SERVICE_NAME_HERE>\"'");
    }
  }

  // make sure this listener is one of the first things run by the agent
  @Override
  public int order() {
    return -100;
  }

  static boolean serviceNameNotConfigured(ConfigProperties config, Resource resource) {
    String serviceName = config.getString("otel.service.name");
    Map<String, String> resourceAttributes = config.getMap("otel.resource.attributes");
    return serviceName == null
        && !resourceAttributes.containsKey(ResourceAttributes.SERVICE_NAME.getKey())
        && "unknown_service:java".equals(resource.getAttribute(ResourceAttributes.SERVICE_NAME));
  }
}
