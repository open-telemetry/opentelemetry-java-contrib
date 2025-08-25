/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.AttributeKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * AttributePropagatingSpanProcessorBuilder is used to construct a {@link
 * AttributePropagatingSpanProcessor}. If {@link #setSpanNamePropagationKey} or {@link
 * #setAttributesKeysToPropagate} are not invoked, the builder defaults to using specific {@link
 * AwsAttributeKeys} as propagation targets.
 */
public final class AttributePropagatingSpanProcessorBuilder {

  private AttributeKey<String> spanNamePropagationKey = AwsAttributeKeys.AWS_LOCAL_OPERATION;
  private List<AttributeKey<String>> attributesKeysToPropagate =
      Arrays.asList(AwsAttributeKeys.AWS_REMOTE_SERVICE, AwsAttributeKeys.AWS_REMOTE_OPERATION);

  public static AttributePropagatingSpanProcessorBuilder create() {
    return new AttributePropagatingSpanProcessorBuilder();
  }

  private AttributePropagatingSpanProcessorBuilder() {}

  @CanIgnoreReturnValue
  public AttributePropagatingSpanProcessorBuilder setSpanNamePropagationKey(
      AttributeKey<String> spanNamePropagationKey) {
    requireNonNull(spanNamePropagationKey, "spanNamePropagationKey");
    this.spanNamePropagationKey = spanNamePropagationKey;
    return this;
  }

  @CanIgnoreReturnValue
  public AttributePropagatingSpanProcessorBuilder setAttributesKeysToPropagate(
      List<AttributeKey<String>> attributesKeysToPropagate) {
    requireNonNull(attributesKeysToPropagate, "attributesKeysToPropagate");
    this.attributesKeysToPropagate = Collections.unmodifiableList(attributesKeysToPropagate);
    return this;
  }

  public AttributePropagatingSpanProcessor build() {
    return AttributePropagatingSpanProcessor.create(
        spanNamePropagationKey, attributesKeysToPropagate);
  }
}
