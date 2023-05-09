/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.AttributeKey;

/** Utility class holding attribute keys with special meaning to AWS components */
final class AwsAttributeKeys {

  private AwsAttributeKeys() {}

  static final AttributeKey<String> AWS_LOCAL_OPERATION =
      AttributeKey.stringKey("aws.local.operation");

  static final AttributeKey<String> AWS_REMOTE_SERVICE =
      AttributeKey.stringKey("aws.remote.service");

  static final AttributeKey<String> AWS_REMOTE_OPERATION =
      AttributeKey.stringKey("aws.remote.operation");
}
