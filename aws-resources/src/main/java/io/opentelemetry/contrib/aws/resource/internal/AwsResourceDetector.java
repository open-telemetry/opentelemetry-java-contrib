/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource.internal;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.aws.resource.BeanstalkResource;
import io.opentelemetry.contrib.aws.resource.Ec2Resource;
import io.opentelemetry.contrib.aws.resource.EcsResource;
import io.opentelemetry.contrib.aws.resource.EksResource;
import io.opentelemetry.contrib.aws.resource.LambdaResource;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * Declarative configuration SPI implementation for AWS resource detection.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class AwsResourceDetector implements ComponentProvider {

  @Override
  public Class<Resource> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return "aws";
  }

  @Override
  public Resource create(DeclarativeConfigProperties config) {
    ResourceBuilder builder = Resource.builder();
    builder.putAll(BeanstalkResource.get());
    builder.putAll(Ec2Resource.get());
    builder.putAll(EcsResource.get());
    builder.putAll(EksResource.get());
    builder.putAll(LambdaResource.get());
    return builder.build();
  }
}
