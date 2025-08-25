/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_ECS_CLUSTER_ARN;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_ECS_CONTAINER_ARN;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_ECS_LAUNCHTYPE;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_ECS_TASK_ARN;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_ECS_TASK_FAMILY;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_ECS_TASK_REVISION;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_LOG_GROUP_ARNS;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_LOG_GROUP_NAMES;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_LOG_STREAM_ARNS;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.AWS_LOG_STREAM_NAMES;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_ACCOUNT_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_AVAILABILITY_ZONE;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_PLATFORM;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_PROVIDER;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_REGION;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CLOUD_RESOURCE_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CONTAINER_ID;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CONTAINER_IMAGE_NAME;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CONTAINER_NAME;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CloudPlatformIncubatingValues.AWS_ECS;
import static io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CloudProviderIncubatingValues.AWS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * A factory for a {@link Resource} which provides information about the current ECS container if
 * running on AWS ECS.
 */
public final class EcsResource {
  private static final Logger logger = Logger.getLogger(EcsResource.class.getName());
  private static final JsonFactory JSON_FACTORY = new JsonFactory();
  private static final String ECS_METADATA_KEY_V4 = "ECS_CONTAINER_METADATA_URI_V4";
  private static final String ECS_METADATA_KEY_V3 = "ECS_CONTAINER_METADATA_URI";

  private static final Resource INSTANCE = buildResource();

  /**
   * Returns a factory for a {@link Resource} which provides information about the current ECS
   * container if running on AWS ECS.
   */
  public static Resource get() {
    return INSTANCE;
  }

  private static Resource buildResource() {
    return buildResource(System.getenv(), new SimpleHttpClient());
  }

  // Visible for testing
  static Resource buildResource(Map<String, String> sysEnv, SimpleHttpClient httpClient) {
    // Note: If V4 is set V3 is set as well, so check V4 first.
    String ecsMetadataUrl =
        sysEnv.getOrDefault(ECS_METADATA_KEY_V4, sysEnv.getOrDefault(ECS_METADATA_KEY_V3, ""));
    if (!ecsMetadataUrl.isEmpty()) {
      AttributesBuilder attrBuilders = Attributes.builder();
      fetchMetadata(httpClient, ecsMetadataUrl, attrBuilders);
      // For TaskARN, Family, Revision.
      // May put the same attribute twice but that shouldn't matter.
      fetchMetadata(httpClient, ecsMetadataUrl + "/task", attrBuilders);
      return Resource.create(attrBuilders.build(), SchemaUrls.V1_25_0);
    }
    // Not running on ECS
    return Resource.empty();
  }

  static void fetchMetadata(
      SimpleHttpClient httpClient, String url, AttributesBuilder attrBuilders) {
    String json = httpClient.fetchString("GET", url, emptyMap(), null);
    if (json.isEmpty()) {
      return;
    }
    attrBuilders.put(CLOUD_PROVIDER, AWS);
    attrBuilders.put(CLOUD_PLATFORM, AWS_ECS);
    try (JsonParser parser = JSON_FACTORY.createParser(json)) {
      parser.nextToken();
      LogArnBuilder logArnBuilder = new LogArnBuilder();
      parseResponse(parser, attrBuilders, logArnBuilder);

      logArnBuilder
          .getLogGroupArn()
          .ifPresent(
              logGroupArn -> {
                attrBuilders.put(AWS_LOG_GROUP_ARNS, singletonList(logGroupArn));
              });

      logArnBuilder
          .getLogStreamArn()
          .ifPresent(
              logStreamArn -> {
                attrBuilders.put(AWS_LOG_STREAM_ARNS, singletonList(logStreamArn));
              });
    } catch (IOException e) {
      logger.log(Level.WARNING, "Can't get ECS metadata", e);
    }
  }

  private static Optional<String> getAccountId(@Nullable String arn) {
    return getArnPart(arn, ArnPart.ACCOUNT);
  }

  private static Optional<String> getRegion(@Nullable String arn) {
    return getArnPart(arn, ArnPart.REGION);
  }

  private static enum ArnPart {
    REGION(3),
    ACCOUNT(4);

    final int partIndex;

    private ArnPart(int partIndex) {
      this.partIndex = partIndex;
    }
  }

  private static Optional<String> getArnPart(@Nullable String arn, ArnPart arnPart) {
    if (arn == null) {
      return Optional.empty();
    }

    String[] arnParts = arn.split(":");

    if (arnPart.partIndex >= arnParts.length) {
      return Optional.empty();
    }

    return Optional.of(arnParts[arnPart.partIndex]);
  }

  // Suppression is required for CONTAINER_IMAGE_TAG until we are ready to upgrade.
  @SuppressWarnings("deprecation")
  static void parseResponse(
      JsonParser parser, AttributesBuilder attrBuilders, LogArnBuilder logArnBuilder)
      throws IOException {
    if (!parser.isExpectedStartObjectToken()) {
      logger.log(Level.WARNING, "Couldn't parse ECS metadata, invalid JSON");
      return;
    }

    // Either the container ARN or the task ARN, they both contain the
    // account id and region tokens we need later for the cloud.account.id
    // and cloud.region attributes.
    String arn = null;
    // Cluster can either be ARN or short name.
    String cluster = null;

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String value = parser.nextTextValue();
      switch (parser.currentName()) {
        case "AvailabilityZone":
          attrBuilders.put(CLOUD_AVAILABILITY_ZONE, value);
          break;
        case "DockerId":
          attrBuilders.put(CONTAINER_ID, value);
          break;
        case "DockerName":
          attrBuilders.put(CONTAINER_NAME, value);
          break;
        case "Cluster":
          cluster = value;
          break;
        case "ContainerARN":
          arn = value;
          attrBuilders.put(AWS_ECS_CONTAINER_ARN, value);
          attrBuilders.put(CLOUD_RESOURCE_ID, value);
          logArnBuilder.setContainerArn(value);
          break;
        case "Image":
          DockerImage parsedImage = DockerImage.parse(value);
          if (parsedImage != null) {
            attrBuilders.put(CONTAINER_IMAGE_NAME, parsedImage.getRepository());
            attrBuilders.put(
                io.opentelemetry.contrib.aws.resource.IncubatingAttributes.CONTAINER_IMAGE_TAGS,
                parsedImage.getTag());
          }
          break;
        case "ImageID":
          attrBuilders.put("aws.ecs.container.image.id", value);
          break;
        case "LogOptions":
          // Recursively parse LogOptions
          parseResponse(parser, attrBuilders, logArnBuilder);
          break;
        case "awslogs-group":
          attrBuilders.put(AWS_LOG_GROUP_NAMES, value);
          logArnBuilder.setLogGroupName(value);
          break;
        case "awslogs-stream":
          attrBuilders.put(AWS_LOG_STREAM_NAMES, value);
          logArnBuilder.setLogStreamName(value);
          break;
        case "awslogs-region":
          logArnBuilder.setRegion(value);
          break;
        case "TaskARN":
          arn = value;
          attrBuilders.put(AWS_ECS_TASK_ARN, value);
          break;
        case "LaunchType":
          attrBuilders.put(AWS_ECS_LAUNCHTYPE, value.toLowerCase(Locale.ROOT));
          break;
        case "Family":
          attrBuilders.put(AWS_ECS_TASK_FAMILY, value);
          break;
        case "Revision":
          attrBuilders.put(AWS_ECS_TASK_REVISION, value);
          break;
        default:
          parser.skipChildren();
          break;
      }
    }

    String region = getRegion(arn).orElse(null);
    String account = getAccountId(arn).orElse(null);
    if (region != null) {
      attrBuilders.put(CLOUD_REGION, region);
    }
    if (account != null) {
      attrBuilders.put(CLOUD_ACCOUNT_ID, account);
    }
    if (cluster != null) {
      if (cluster.contains(":")) {
        attrBuilders.put(AWS_ECS_CLUSTER_ARN, cluster);
      } else {
        String clusterArn = String.format("arn:aws:ecs:%s:%s:cluster/%s", region, account, cluster);
        attrBuilders.put(AWS_ECS_CLUSTER_ARN, clusterArn);
      }
    }
  }

  private EcsResource() {}

  /**
   * This builder can piece together the ARN of a log group or a log stream from region, account,
   * group name and stream name as the ARN isn't part of the ECS metadata.
   *
   * <p>If we just set AWS_LOG_GROUP_NAMES then the CloudWatch X-Ray traces view displays "An error
   * occurred fetching your data". That's why it's important we set the ARN.
   */
  private static class LogArnBuilder {

    @Nullable String region;
    @Nullable String account;
    @Nullable String logGroupName;
    @Nullable String logStreamName;

    void setRegion(@Nullable String region) {
      this.region = region;
    }

    void setLogGroupName(@Nullable String logGroupName) {
      this.logGroupName = logGroupName;
    }

    void setLogStreamName(@Nullable String logStreamName) {
      this.logStreamName = logStreamName;
    }

    void setContainerArn(@Nullable String containerArn) {
      account = getAccountId(containerArn).orElse(null);
    }

    Optional<String> getLogGroupArn() {
      if (region == null || account == null || logGroupName == null) {
        return Optional.empty();
      }

      return Optional.of("arn:aws:logs:" + region + ":" + account + ":log-group:" + logGroupName);
    }

    Optional<String> getLogStreamArn() {
      if (region == null || account == null || logGroupName == null || logStreamName == null) {
        return Optional.empty();
      }

      return Optional.of(
          "arn:aws:logs:"
              + region
              + ":"
              + account
              + ":log-group:"
              + logGroupName
              + ":log-stream:"
              + logStreamName);
    }
  }

  /** This can parse a Docker image name into its parts: repository, tag and sha256. */
  private static class DockerImage {

    private static final Pattern imagePattern =
        Pattern.compile(
            "^(?<repository>([^/\\s]+/)?([^:\\s]+))(:(?<tag>[^@\\s]+))?(@sha256:(?<sha256>[\\da-fA-F]+))?$");

    final String repository;
    final String tag;

    private DockerImage(String repository, String tag) {
      this.repository = repository;
      this.tag = tag;
    }

    String getRepository() {
      return repository;
    }

    String getTag() {
      return tag;
    }

    @Nullable
    static DockerImage parse(@Nullable String image) {
      if (image == null || image.isEmpty()) {
        return null;
      }
      Matcher matcher = imagePattern.matcher(image);
      if (!matcher.matches()) {
        logger.log(Level.WARNING, "Couldn't parse image '" + image + "'");
        return null;
      }
      String repository = matcher.group("repository");
      String tag = matcher.group("tag");
      if (tag == null || tag.isEmpty()) {
        tag = "latest";
      }
      return new DockerImage(repository, tag);
    }
  }
}
