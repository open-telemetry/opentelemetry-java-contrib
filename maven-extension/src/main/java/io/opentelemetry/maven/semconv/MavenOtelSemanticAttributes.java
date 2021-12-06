/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.semconv;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

/**
 * @see io.opentelemetry.api.common.Attributes
 * @see io.opentelemetry.semconv.trace.attributes.SemanticAttributes
 */
public class MavenOtelSemanticAttributes {
  public static final AttributeKey<String> MAVEN_EXECUTION_ID = stringKey("maven.execution.id");
  public static final AttributeKey<String> MAVEN_PROJECT_ARTIFACT_ID =
      stringKey("maven.project.artifactId");
  public static final AttributeKey<String> MAVEN_PROJECT_GROUP_ID =
      stringKey("maven.project.groupId");
  public static final AttributeKey<String> MAVEN_PROJECT_VERSION =
      stringKey("maven.project.version");
  public static final AttributeKey<String> MAVEN_PLUGIN_ARTIFACT_ID =
      stringKey("maven.plugin.artifactId");
  public static final AttributeKey<String> MAVEN_PLUGIN_GROUP_ID =
      stringKey("maven.plugin.groupId");
  public static final AttributeKey<String> MAVEN_PLUGIN_VERSION = stringKey("maven.plugin.version");
  public static final AttributeKey<String> MAVEN_EXECUTION_GOAL = stringKey("maven.execution.goal");
  public static final AttributeKey<String> MAVEN_EXECUTION_LIFECYCLE_PHASE =
      stringKey("maven.execution.lifecyclePhase");
  public static final AttributeKey<String> MAVEN_REPOSITORY_ID = stringKey("maven.repository.id");
  public static final AttributeKey<String> MAVEN_REPOSITORY_URL = stringKey("maven.repository.url");
  public static final AttributeKey<String> MAVEN_REPOSITORY_USERNAME =
      stringKey("maven.repository.authentication.username");

  public static final String SERVICE_NAME_VALUE = "maven";
}
