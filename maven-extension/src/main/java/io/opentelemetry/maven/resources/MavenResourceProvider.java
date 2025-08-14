/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.resources;

import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenResourceProvider implements ResourceProvider {

  private static final Logger logger = LoggerFactory.getLogger(MavenResourceProvider.class);

  @Override
  public Resource createResource(ConfigProperties config) {
    return create();
  }

  static Resource create() {
    return Resource.builder()
        .put(ServiceAttributes.SERVICE_NAME, MavenOtelSemanticAttributes.SERVICE_NAME_VALUE)
        .put(ServiceAttributes.SERVICE_VERSION, getMavenRuntimeVersion())
        .put(
            MavenOtelSemanticAttributes.TELEMETRY_DISTRO_NAME,
            MavenOtelSemanticAttributes.TELEMETRY_DISTRO_NAME_VALUE)
        .put(
            MavenOtelSemanticAttributes.TELEMETRY_DISTRO_VERSION,
            MavenOtelSemanticAttributes.TELEMETRY_DISTRO_VERSION_VALUE)
        .build();
  }

  /**
   * Recopy of <a
   * href="https://github.com/apache/maven/blob/maven-4.0.0-rc-2/compat/maven-compat/src/main/java/org/apache/maven/execution/DefaultRuntimeInformation.java">
   * <code>org.apache.maven.rtinfo.internal.DefaultRuntimeInformation#getMavenVersion()</code></a>
   * that is not available in Maven 4.0+
   */
  static String getMavenRuntimeVersion() {
    String mavenVersion;
    Properties props = new Properties();
    String resource = "META-INF/maven/org.apache.maven/maven-core/pom.properties";

    try (InputStream is = Maven.class.getResourceAsStream("/" + resource)) {
      if (is != null) {
        props.load(is);
      } else {
        logger.warn(
            "Could not locate {} on classpath, Maven runtime information not available", resource);
      }
    } catch (IOException e) {
      String msg = "Could not parse " + resource + ", Maven runtime information not available";
      if (logger.isDebugEnabled()) {
        logger.warn(msg, e);
      } else {
        logger.warn(msg);
      }
    }

    String version = props.getProperty("version", "").trim();
    if (!version.startsWith("${")) {
      mavenVersion = version;
    } else {
      mavenVersion = "";
    }
    return mavenVersion;
  }
}
