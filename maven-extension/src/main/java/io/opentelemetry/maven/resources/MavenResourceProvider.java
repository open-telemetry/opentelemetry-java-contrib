/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.resources;

import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.rtinfo.internal.DefaultRuntimeInformation;

public class MavenResourceProvider implements ResourceProvider {
  @Override
  public Resource createResource(ConfigProperties config) {
    // TODO verify if there is solution to retrieve the RuntimeInformation instance loaded by the
    //  Maven Plexus Launcher
    RuntimeInformation runtimeInformation = new DefaultRuntimeInformation();
    return Resource.builder()
        .put(ResourceAttributes.SERVICE_NAME, MavenOtelSemanticAttributes.SERVICE_NAME_VALUE)
        .put(ResourceAttributes.SERVICE_VERSION, runtimeInformation.getMavenVersion())
        .build();
  }
}
