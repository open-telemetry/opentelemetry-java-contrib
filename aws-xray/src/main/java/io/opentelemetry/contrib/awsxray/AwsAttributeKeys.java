/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.AttributeKey;

/** Utility class holding attribute keys with special meaning to AWS components */
final class AwsAttributeKeys {

  private AwsAttributeKeys() {}

  static final AttributeKey<String> AWS_SPAN_KIND = AttributeKey.stringKey("aws.span.kind");

  static final AttributeKey<String> AWS_LOCAL_SERVICE = AttributeKey.stringKey("aws.local.service");

  static final AttributeKey<String> AWS_LOCAL_OPERATION =
      AttributeKey.stringKey("aws.local.operation");

  static final AttributeKey<String> AWS_REMOTE_SERVICE =
      AttributeKey.stringKey("aws.remote.service");

  static final AttributeKey<String> AWS_REMOTE_OPERATION =
      AttributeKey.stringKey("aws.remote.operation");

  static final AttributeKey<String> AWS_REMOTE_TARGET = AttributeKey.stringKey("aws.remote.target");

  // use the same AWS Resource attribute name defined by OTel java auto-instr for aws_sdk_v_1_1
  static final AttributeKey<String> AWS_BUCKET_NAME = AttributeKey.stringKey("aws.bucket.name");
  static final AttributeKey<String> AWS_QUEUE_NAME = AttributeKey.stringKey("aws.queue.name");
  static final AttributeKey<String> AWS_STREAM_NAME = AttributeKey.stringKey("aws.stream.name");
  static final AttributeKey<String> AWS_TABLE_NAME = AttributeKey.stringKey("aws.table.name");
}
