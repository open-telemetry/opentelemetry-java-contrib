/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

/** Utility class holding attribute keys with special meaning to AWS components */
final class AwsAttributeKeys {

  private AwsAttributeKeys() {}

  static final AttributeKey<String> AWS_SPAN_KIND = stringKey("aws.span.kind");

  static final AttributeKey<String> AWS_LOCAL_SERVICE = stringKey("aws.local.service");

  static final AttributeKey<String> AWS_LOCAL_OPERATION = stringKey("aws.local.operation");

  static final AttributeKey<String> AWS_REMOTE_SERVICE = stringKey("aws.remote.service");

  static final AttributeKey<String> AWS_REMOTE_OPERATION = stringKey("aws.remote.operation");

  static final AttributeKey<String> AWS_REMOTE_TARGET = stringKey("aws.remote.target");

  // use the same AWS Resource attribute name defined by OTel java auto-instr for aws_sdk_v_1_1
  // TODO: all AWS specific attributes should be defined in semconv package and reused cross all
  // otel packages. Related sim -
  // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/8710

  static final AttributeKey<String> AWS_BUCKET_NAME = stringKey("aws.bucket.name");
  static final AttributeKey<String> AWS_QUEUE_NAME = stringKey("aws.queue.name");
  static final AttributeKey<String> AWS_STREAM_NAME = stringKey("aws.stream.name");
  static final AttributeKey<String> AWS_TABLE_NAME = stringKey("aws.table.name");
}
