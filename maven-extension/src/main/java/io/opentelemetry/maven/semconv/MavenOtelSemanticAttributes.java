/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.semconv;

import static io.opentelemetry.api.common.AttributeKey.stringArrayKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.maven.OpenTelemetrySdkService;
import java.util.List;
import org.eclipse.aether.transfer.TransferEvent;

/**
 * Semantic attributes for Maven executions.
 *
 * @see io.opentelemetry.api.common.Attributes
 */
public class MavenOtelSemanticAttributes {

  /**
   * See {@code
   * io.opentelemetry.semconv.incubating.ContainerIncubatingAttributes#CONTAINER_IMAGE_NAME}
   */
  public static final AttributeKey<String> MAVEN_BUILD_CONTAINER_IMAGE_NAME =
      stringKey("maven.build.container.image.name");

  /** See {@code ContainerIncubatingAttributes#CONTAINER_IMAGE_TAGS} */
  public static final AttributeKey<List<String>> MAVEN_BUILD_CONTAINER_IMAGE_TAGS =
      stringArrayKey("maven.build.container.image.tags");

  public static final AttributeKey<String> MAVEN_BUILD_CONTAINER_REGISTRY_URL =
      stringKey("maven.build.container.registry.url");
  public static final AttributeKey<String> MAVEN_BUILD_REPOSITORY_ID =
      stringKey("maven.build.repository.id");
  public static final AttributeKey<String> MAVEN_BUILD_REPOSITORY_URL =
      stringKey("maven.build.repository.url");

  public static final AttributeKey<String> MAVEN_EXECUTION_GOAL = stringKey("maven.execution.goal");

  public static final AttributeKey<String> MAVEN_EXECUTION_ID = stringKey("maven.execution.id");
  public static final AttributeKey<String> MAVEN_EXECUTION_LIFECYCLE_PHASE =
      stringKey("maven.execution.lifecyclePhase");
  public static final AttributeKey<String> MAVEN_PLUGIN_ARTIFACT_ID =
      stringKey("maven.plugin.artifactId");
  public static final AttributeKey<String> MAVEN_PLUGIN_GROUP_ID =
      stringKey("maven.plugin.groupId");
  public static final AttributeKey<String> MAVEN_PLUGIN_VERSION = stringKey("maven.plugin.version");
  public static final AttributeKey<String> MAVEN_PROJECT_ARTIFACT_ID =
      stringKey("maven.project.artifactId");
  public static final AttributeKey<String> MAVEN_PROJECT_GROUP_ID =
      stringKey("maven.project.groupId");
  public static final AttributeKey<String> MAVEN_PROJECT_VERSION =
      stringKey("maven.project.version");

  /** See {@link TransferEvent.RequestType}. */
  public static final AttributeKey<String> MAVEN_TRANSFER_TYPE =
      AttributeKey.stringKey("maven.transfer.type");
  /** See {@link org.eclipse.aether.transfer.TransferResource}. */
  public static final AttributeKey<String> MAVEN_RESOURCE_NAME =
      AttributeKey.stringKey("maven.resource.name");

  public static final String SERVICE_NAME_VALUE = "maven";

  // inlined incubating attribute to prevent direct dependency on incubating semconv
  public static final AttributeKey<String> PEER_SERVICE = AttributeKey.stringKey("peer.service");
  public static final AttributeKey<String> RPC_METHOD = AttributeKey.stringKey("rpc.method");
  public static final AttributeKey<String> TELEMETRY_DISTRO_NAME =
      AttributeKey.stringKey("telemetry.distro.name");
  public static final AttributeKey<String> TELEMETRY_DISTRO_VERSION =
      AttributeKey.stringKey("telemetry.distro.version");

  public static final String TELEMETRY_DISTRO_NAME_VALUE = "opentelemetry-maven-extension";

  public static final String TELEMETRY_DISTRO_VERSION_VALUE =
      OpenTelemetrySdkService.class.getPackage().getImplementationVersion();

  private MavenOtelSemanticAttributes() {}
}
