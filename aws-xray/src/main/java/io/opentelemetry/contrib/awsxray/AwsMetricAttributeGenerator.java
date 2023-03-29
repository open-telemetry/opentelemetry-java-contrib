/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_LOCAL_OPERATION;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_LOCAL_SERVICE;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_REMOTE_OPERATION;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_REMOTE_SERVICE;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_SPAN_KIND;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.FAAS_INVOKED_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.FAAS_INVOKED_PROVIDER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.GRAPHQL_OPERATION_TYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_SYSTEM;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.PEER_SERVICE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_SERVICE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AwsMetricAttributeGenerator generates very specific metric attributes based on low-cardinality
 * span and resource attributes. If such attributes are not present, we fallback to default values.
 *
 * <p>The goal of these particular metric attributes is to get metrics for incoming and outgoing
 * traffic for a service. Namely, {@link SpanKind#SERVER} and {@link SpanKind#CONSUMER} spans
 * represent "incoming" traffic, {@link SpanKind#CLIENT} and {@link SpanKind#PRODUCER} spans
 * represent "outgoing" traffic, and {@link SpanKind#INTERNAL} spans are ignored.
 */
final class AwsMetricAttributeGenerator implements MetricAttributeGenerator {

  private static final Logger logger =
      Logger.getLogger(AwsMetricAttributeGenerator.class.getName());

  // Special SERVICE attribute value if GRAPHQL_OPERATION_TYPE attribute key is present.
  private static final String GRAPHQL = "graphql";

  // Default attribute values if no valid span attribute value is identified
  private static final String UNKNOWN_SERVICE = "UnknownService";
  private static final String UNKNOWN_OPERATION = "UnknownOperation";
  private static final String UNKNOWN_REMOTE_SERVICE = "UnknownRemoteService";
  private static final String UNKNOWN_REMOTE_OPERATION = "UnknownRemoteOperation";

  @Override
  public Attributes generateMetricAttributesFromSpan(SpanData span, Resource resource) {
    AttributesBuilder builder = Attributes.builder();
    switch (span.getKind()) {
      case CONSUMER:
      case SERVER:
        setService(resource, span, builder);
        setIngressOperation(span, builder);
        setSpanKind(span, builder);
        break;
      case PRODUCER:
      case CLIENT:
        setService(resource, span, builder);
        setEgressOperation(span, builder);
        setRemoteServiceAndOperation(span, builder);
        setSpanKind(span, builder);
        break;
      default:
        // Add no attributes, signalling no metrics should be emitted.
    }
    return builder.build();
  }

  /** Service is always derived from {@link ResourceAttributes#SERVICE_NAME} */
  private static void setService(Resource resource, SpanData span, AttributesBuilder builder) {
    String service = resource.getAttribute(SERVICE_NAME);
    if (service == null) {
      logUnknownAttribute(AWS_LOCAL_SERVICE, span);
      service = UNKNOWN_SERVICE;
    }
    builder.put(AWS_LOCAL_SERVICE, service);
  }

  /**
   * Ingress operation (i.e. operation for Server and Consumer spans) is always derived from span
   * name.
   */
  private static void setIngressOperation(SpanData span, AttributesBuilder builder) {
    String operation = span.getName();
    if (operation == null) {
      logUnknownAttribute(AWS_LOCAL_OPERATION, span);
      operation = UNKNOWN_OPERATION;
    }
    builder.put(AWS_LOCAL_OPERATION, operation);
  }

  /**
   * Egress operation (i.e. operation for Client and Producer spans) is always derived from a
   * special span attribute, {@link AwsAttributeKeys#AWS_LOCAL_OPERATION}. This attribute is
   * generated with a separate SpanProcessor, {@link AttributePropagatingSpanProcessor}
   */
  private static void setEgressOperation(SpanData span, AttributesBuilder builder) {
    String operation = span.getAttributes().get(AWS_LOCAL_OPERATION);
    if (operation == null) {
      logUnknownAttribute(AWS_LOCAL_OPERATION, span);
      operation = UNKNOWN_OPERATION;
    }
    builder.put(AWS_LOCAL_OPERATION, operation);
  }

  /**
   * Remote attributes (only for Client and Producer spans) are generated based on low-cardinality
   * span attributes, in priority order.
   *
   * <p>The first priority is the AWS Remote attributes, which are generated from manually
   * instrumented span attributes, and are clear indications of customer intent. If AWS Remote
   * attributes are not present, the next highest priority span attribute is Peer Service, which is
   * also a reliable indicator of customer intent. If this is set, it will override
   * AWS_REMOTE_SERVICE identified from any other span attribute, other than AWS Remote attributes.
   *
   * <p>After this, we look for the following low-cardinality span attributes that can be used to
   * determine the remote metric attributes:
   *
   * <ul>
   *   <li>RPC
   *   <li>DB
   *   <li>FAAS
   *   <li>Messaging
   *   <li>GraphQL - Special case, if {@link SemanticAttributes#GRAPHQL_OPERATION_TYPE} is present,
   *       we use it for RemoteOperation and set RemoteService to {@link #GRAPHQL}.
   * </ul>
   *
   * <p>In each case, these span attributes were selected from the OpenTelemetry trace semantic
   * convention specifications as they adhere to the three following criteria:
   *
   * <ul>
   *   <li>Attributes are meaningfully indicative of remote service/operation names.
   *   <li>Attributes are defined in the specification to be low cardinality, usually with a low-
   *       cardinality list of values.
   *   <li>Attributes are confirmed to have low-cardinality values, based on code analysis.
   * </ul>
   *
   * TODO: This specific logic may change in future. Specifically, we are still deciding which HTTP
   * and RPC attributes to use here, but this is a sufficient starting point.
   */
  private static void setRemoteServiceAndOperation(SpanData span, AttributesBuilder builder) {
    if (isKeyPresent(span, AWS_REMOTE_SERVICE) || isKeyPresent(span, AWS_REMOTE_OPERATION)) {
      setRemoteService(span, builder, AWS_REMOTE_SERVICE);
      setRemoteOperation(span, builder, AWS_REMOTE_OPERATION);
    } else if (isKeyPresent(span, RPC_SERVICE) || isKeyPresent(span, RPC_METHOD)) {
      setRemoteService(span, builder, RPC_SERVICE);
      setRemoteOperation(span, builder, RPC_METHOD);
    } else if (isKeyPresent(span, DB_SYSTEM) || isKeyPresent(span, DB_OPERATION)) {
      setRemoteService(span, builder, DB_SYSTEM);
      setRemoteOperation(span, builder, DB_OPERATION);
    } else if (isKeyPresent(span, FAAS_INVOKED_PROVIDER) || isKeyPresent(span, FAAS_INVOKED_NAME)) {
      setRemoteService(span, builder, FAAS_INVOKED_PROVIDER);
      setRemoteOperation(span, builder, FAAS_INVOKED_NAME);
    } else if (isKeyPresent(span, MESSAGING_SYSTEM) || isKeyPresent(span, MESSAGING_OPERATION)) {
      setRemoteService(span, builder, MESSAGING_SYSTEM);
      setRemoteOperation(span, builder, MESSAGING_OPERATION);
    } else if (isKeyPresent(span, GRAPHQL_OPERATION_TYPE)) {
      builder.put(AWS_REMOTE_SERVICE, GRAPHQL);
      setRemoteOperation(span, builder, GRAPHQL_OPERATION_TYPE);
    } else {
      logUnknownAttribute(AWS_REMOTE_SERVICE, span);
      builder.put(AWS_REMOTE_SERVICE, UNKNOWN_REMOTE_SERVICE);
      logUnknownAttribute(AWS_REMOTE_OPERATION, span);
      builder.put(AWS_REMOTE_OPERATION, UNKNOWN_REMOTE_OPERATION);
    }

    // Peer service takes priority as RemoteService over everything but AWS Remote.
    if (isKeyPresent(span, PEER_SERVICE) && !isKeyPresent(span, AWS_REMOTE_SERVICE)) {
      setRemoteService(span, builder, PEER_SERVICE);
    }
  }

  /** Span kind is needed for differentiating metrics in the EMF exporter */
  private static void setSpanKind(SpanData span, AttributesBuilder builder) {
    String spanKind = span.getKind().name();
    builder.put(AWS_SPAN_KIND, spanKind);
  }

  private static boolean isKeyPresent(SpanData span, AttributeKey<String> key) {
    return span.getAttributes().get(key) != null;
  }

  private static void setRemoteService(
      SpanData span, AttributesBuilder builder, AttributeKey<String> remoteServiceKey) {
    String remoteService = span.getAttributes().get(remoteServiceKey);
    if (remoteService == null) {
      logUnknownAttribute(AWS_REMOTE_SERVICE, span);
      remoteService = UNKNOWN_REMOTE_SERVICE;
    }
    builder.put(AWS_REMOTE_SERVICE, remoteService);
  }

  private static void setRemoteOperation(
      SpanData span, AttributesBuilder builder, AttributeKey<String> remoteOperationKey) {
    String remoteOperation = span.getAttributes().get(remoteOperationKey);
    if (remoteOperation == null) {
      logUnknownAttribute(AWS_REMOTE_OPERATION, span);
      remoteOperation = UNKNOWN_REMOTE_OPERATION;
    }
    builder.put(AWS_REMOTE_OPERATION, remoteOperation);
  }

  private static void logUnknownAttribute(AttributeKey<String> attributeKey, SpanData span) {
    String[] params = {
      attributeKey.getKey(), span.getKind().name(), span.getSpanContext().getSpanId()
    };
    logger.log(Level.FINEST, "No valid {0} value found for {1} span {2}", params);
  }
}
