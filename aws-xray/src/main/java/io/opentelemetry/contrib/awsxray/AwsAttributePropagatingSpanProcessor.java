/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * AwsAttributePropagatingSpanProcessor handles the creation of the aws.local.operation attribute,
 * and the inheritance of the {@link AwsAttributeKeys#AWS_LOCAL_OPERATION}, {@link
 * AwsAttributeKeys#AWS_REMOTE_APPLICATION} and {@link AwsAttributeKeys#AWS_REMOTE_OPERATION}.
 *
 * <p>The {@link AwsAttributeKeys#AWS_LOCAL_OPERATION} is created when a local root span of SERVER
 * or CONSUMER type is found. The span name will be used as the attribute value. The {@link
 * AwsAttributeKeys#AWS_REMOTE_APPLICATION} and {@link AwsAttributeKeys#AWS_REMOTE_OPERATION}
 * attributes must be created by manual instrumentation. These attributes will be copied from the
 * parent span to children after receiving them, and later be used in SpanMetricsProcessor to help
 * generate metric attributes.
 */
public final class AwsAttributePropagatingSpanProcessor implements SpanProcessor {

  private AwsAttributePropagatingSpanProcessor() {}

  public static AwsAttributePropagatingSpanProcessor create() {
    return new AwsAttributePropagatingSpanProcessor();
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    String localOperation;
    if (isLocalRoot(parentReadableSpan.getParentSpanContext())
        && isServerOrConsumer(parentReadableSpan)) {
      localOperation = parentReadableSpan.getName();
    } else {
      localOperation = parentReadableSpan.getAttribute(AwsAttributeKeys.AWS_LOCAL_OPERATION);
    }
    if (localOperation != null) {
      span.setAttribute(AwsAttributeKeys.AWS_LOCAL_OPERATION, localOperation);
    }

    String remoteApplication =
        parentReadableSpan.getAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION);
    if (remoteApplication != null) {
      span.setAttribute(AwsAttributeKeys.AWS_REMOTE_APPLICATION, remoteApplication);
    }

    String remoteOperation = parentReadableSpan.getAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION);
    if (remoteOperation != null) {
      span.setAttribute(AwsAttributeKeys.AWS_REMOTE_OPERATION, remoteOperation);
    }
  }

  private static boolean isLocalRoot(SpanContext parentSpanContext) {
    return !parentSpanContext.isValid() || parentSpanContext.isRemote();
  }

  private static boolean isServerOrConsumer(ReadableSpan span) {
    return span.getKind() == SpanKind.SERVER || span.getKind() == SpanKind.CONSUMER;
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
