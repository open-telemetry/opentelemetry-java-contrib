/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import io.opentelemetry.api.common.AttributeKey;
import java.util.List;

/**
 * Inlines incubating attributes until they are stable, doing this prevents having a direct
 * dependency on incubating artifact which can conflict with another incubating version.
 */
class IncubatingAttributes {
  private IncubatingAttributes() {}

  public static final AttributeKey<String> CLOUD_ACCOUNT_ID =
      AttributeKey.stringKey("cloud.account.id");
  public static final AttributeKey<String> CLOUD_AVAILABILITY_ZONE =
      AttributeKey.stringKey("cloud.availability_zone");
  public static final AttributeKey<String> CLOUD_PLATFORM =
      AttributeKey.stringKey("cloud.platform");
  public static final AttributeKey<String> CLOUD_PROVIDER =
      AttributeKey.stringKey("cloud.provider");
  public static final AttributeKey<String> CLOUD_REGION = AttributeKey.stringKey("cloud.region");
  public static final AttributeKey<String> CLOUD_RESOURCE_ID =
      AttributeKey.stringKey("cloud.resource_id");

  public static final class CloudPlatformValues {
    public static final String AWS_EC2 = "aws_ec2";
    public static final String AWS_ECS = "aws_ecs";
    public static final String AWS_EKS = "aws_eks";
    public static final String AWS_LAMBDA = "aws_lambda";
    public static final String AWS_ELASTIC_BEANSTALK = "aws_elastic_beanstalk";

    private CloudPlatformValues() {}
  }

  public static final class CloudProviderValues {
    public static final String AWS = "aws";

    private CloudProviderValues() {}
  }

  public static final AttributeKey<String> SERVICE_INSTANCE_ID =
      AttributeKey.stringKey("service.instance.id");
  public static final AttributeKey<String> SERVICE_NAMESPACE =
      AttributeKey.stringKey("service.namespace");

  public static final AttributeKey<String> HOST_ID = AttributeKey.stringKey("host.id");
  public static final AttributeKey<String> HOST_IMAGE_ID = AttributeKey.stringKey("host.image.id");
  public static final AttributeKey<String> HOST_NAME = AttributeKey.stringKey("host.name");
  public static final AttributeKey<String> HOST_TYPE = AttributeKey.stringKey("host.type");

  public static final AttributeKey<String> CONTAINER_ID = AttributeKey.stringKey("container.id");
  public static final AttributeKey<String> CONTAINER_IMAGE_NAME =
      AttributeKey.stringKey("container.image.name");
  public static final AttributeKey<String> CONTAINER_NAME =
      AttributeKey.stringKey("container.name");

  public static final AttributeKey<String> K8S_CLUSTER_NAME =
      AttributeKey.stringKey("k8s.cluster.name");

  public static final AttributeKey<String> AWS_ECS_CONTAINER_ARN =
      AttributeKey.stringKey("aws.ecs.container.arn");
  public static final AttributeKey<String> AWS_ECS_LAUNCHTYPE =
      AttributeKey.stringKey("aws.ecs.launchtype");
  public static final AttributeKey<String> AWS_ECS_TASK_ARN =
      AttributeKey.stringKey("aws.ecs.task.arn");
  public static final AttributeKey<String> AWS_ECS_TASK_FAMILY =
      AttributeKey.stringKey("aws.ecs.task.family");
  public static final AttributeKey<String> AWS_ECS_TASK_REVISION =
      AttributeKey.stringKey("aws.ecs.task.revision");
  public static final AttributeKey<List<String>> AWS_LOG_GROUP_ARNS =
      AttributeKey.stringArrayKey("aws.log.group.arns");
  public static final AttributeKey<List<String>> AWS_LOG_GROUP_NAMES =
      AttributeKey.stringArrayKey("aws.log.group.names");
  public static final AttributeKey<List<String>> AWS_LOG_STREAM_ARNS =
      AttributeKey.stringArrayKey("aws.log.stream.arns");
  public static final AttributeKey<List<String>> AWS_LOG_STREAM_NAMES =
      AttributeKey.stringArrayKey("aws.log.stream.names");

  public static final AttributeKey<String> FAAS_NAME = AttributeKey.stringKey("faas.name");
  public static final AttributeKey<String> FAAS_VERSION = AttributeKey.stringKey("faas.version");
}
