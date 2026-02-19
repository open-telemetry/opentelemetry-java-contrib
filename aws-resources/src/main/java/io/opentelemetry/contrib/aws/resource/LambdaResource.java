/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_ACCOUNT_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AWS_LAMBDA;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CloudProviderIncubatingValues.AWS;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.FAAS_NAME;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.FAAS_VERSION;
import static java.util.logging.Level.FINE;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** A factory for a {@link Resource} which provides information about the AWS Lambda function. */
public final class LambdaResource {

  private static final Logger logger = Logger.getLogger(LambdaResource.class.getName());

  private static final String ACCOUNT_ID_SYMLINK_PATH = "/tmp/.otel-aws-account-id";

  private static final Resource INSTANCE = buildResource();

  /**
   * Returns a factory for a {@link Resource} which provides information about the AWS Lambda
   * function.
   */
  public static Resource get() {
    return INSTANCE;
  }

  private static Resource buildResource() {
    return buildResource(System.getenv(), Paths.get(ACCOUNT_ID_SYMLINK_PATH));
  }

  // Visible for testing
  static Resource buildResource(Map<String, String> environmentVariables) {
    return buildResource(environmentVariables, Paths.get(ACCOUNT_ID_SYMLINK_PATH));
  }

  // Visible for testing
  static Resource buildResource(Map<String, String> environmentVariables, Path accountIdSymlink) {
    String region = environmentVariables.getOrDefault("AWS_REGION", "");
    String functionName = environmentVariables.getOrDefault("AWS_LAMBDA_FUNCTION_NAME", "");
    String functionVersion = environmentVariables.getOrDefault("AWS_LAMBDA_FUNCTION_VERSION", "");

    if (!isLambda(functionName, functionVersion)) {
      return Resource.empty();
    }

    AttributesBuilder builder = Attributes.builder().put(CLOUD_PROVIDER, AWS);
    builder.put(CLOUD_PLATFORM, AWS_LAMBDA);

    if (!region.isEmpty()) {
      builder.put(CLOUD_REGION, region);
    }
    if (!functionName.isEmpty()) {
      builder.put(FAAS_NAME, functionName);
    }
    if (!functionVersion.isEmpty()) {
      builder.put(FAAS_VERSION, functionVersion);
    }

    try {
      String accountId = Files.readSymbolicLink(accountIdSymlink).toString();
      if (!accountId.isEmpty()) {
        builder.put(CLOUD_ACCOUNT_ID, accountId);
      }
    } catch (IOException | UnsupportedOperationException e) {
      logger.log(FINE, "cloud.account.id not available via symlink", e);
    }

    return Resource.create(builder.build(), SchemaUrls.V1_25_0);
  }

  private static boolean isLambda(String... envVariables) {
    return Stream.of(envVariables).anyMatch(v -> !v.isEmpty());
  }

  private LambdaResource() {}
}
