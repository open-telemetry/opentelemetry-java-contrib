/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_BUCKET_NAME;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_LOCAL_OPERATION;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_LOCAL_SERVICE;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_QUEUE_NAME;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_REMOTE_OPERATION;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_REMOTE_SERVICE;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_REMOTE_TARGET;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_SPAN_KIND;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_STREAM_NAME;
import static io.opentelemetry.contrib.awsxray.AwsAttributeKeys.AWS_TABLE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_INVOKED_NAME;
import static io.opentelemetry.semconv.incubating.FaasIncubatingAttributes.FAAS_TRIGGER;
import static io.opentelemetry.semconv.incubating.GraphqlIncubatingAttributes.GRAPHQL_OPERATION_TYPE;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_URL;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static io.opentelemetry.semconv.incubating.NetIncubatingAttributes.NET_PEER_NAME;
import static io.opentelemetry.semconv.incubating.NetIncubatingAttributes.NET_PEER_PORT;
import static io.opentelemetry.semconv.incubating.NetIncubatingAttributes.NET_SOCK_PEER_ADDR;
import static io.opentelemetry.semconv.incubating.NetIncubatingAttributes.NET_SOCK_PEER_PORT;
import static io.opentelemetry.semconv.incubating.PeerIncubatingAttributes.PEER_SERVICE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SERVICE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.incubating.GraphqlIncubatingAttributes;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
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
@SuppressWarnings("deprecation") // uses deprecated semantic conventions
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
        setRemoteTarget(span, builder);
        setSpanKind(span, builder);
        break;
      default:
        // Add no attributes, signalling no metrics should be emitted.
    }
    return builder.build();
  }

  private static void setRemoteTarget(SpanData span, AttributesBuilder builder) {
    Optional<String> remoteTarget = getRemoteTarget(span);
    remoteTarget.ifPresent(s -> builder.put(AWS_REMOTE_TARGET, s));
  }

  /**
   * RemoteTarget attribute {@link AwsAttributeKeys#AWS_REMOTE_TARGET} is used to store the resource
   * name of the remote invokes, such as S3 bucket name, mysql table name, etc. TODO: currently only
   * support AWS resource name, will be extended to support the general remote targets, such as
   * ActiveMQ name, etc.
   */
  private static Optional<String> getRemoteTarget(SpanData span) {
    if (isKeyPresent(span, AWS_BUCKET_NAME)) {
      return Optional.ofNullable(span.getAttributes().get(AWS_BUCKET_NAME));
    } else if (isKeyPresent(span, AWS_QUEUE_NAME)) {
      return Optional.ofNullable(span.getAttributes().get(AWS_QUEUE_NAME));
    } else if (isKeyPresent(span, AWS_STREAM_NAME)) {
      return Optional.ofNullable(span.getAttributes().get(AWS_STREAM_NAME));
    } else if (isKeyPresent(span, AWS_TABLE_NAME)) {
      return Optional.ofNullable(span.getAttributes().get(AWS_TABLE_NAME));
    }
    return Optional.empty();
  }

  /** Service is always derived from {@link ServiceAttributes#SERVICE_NAME} */
  private static void setService(Resource resource, SpanData span, AttributesBuilder builder) {
    String service = resource.getAttribute(SERVICE_NAME);
    if (service == null) {
      logUnknownAttribute(AWS_LOCAL_SERVICE, span);
      service = UNKNOWN_SERVICE;
    }
    builder.put(AWS_LOCAL_SERVICE, service);
  }

  /**
   * Ingress operation (i.e. operation for Server and Consumer spans) will be generated from
   * "http.method + http.target/with the first API path parameter" if the default span name equals
   * null, UnknownOperation or http.method value.
   */
  private static void setIngressOperation(SpanData span, AttributesBuilder builder) {
    String operation;
    if (!isValidOperation(span)) {
      operation = generateIngressOperation(span);
    } else {
      operation = span.getName();
    }
    if (operation.equals(UNKNOWN_OPERATION)) {
      logUnknownAttribute(AWS_LOCAL_OPERATION, span);
    }
    builder.put(AWS_LOCAL_OPERATION, operation);
  }

  /**
   * When Span name is null, UnknownOperation or HttpMethod value, it will be treated as invalid
   * local operation value that needs to be further processed
   */
  private static boolean isValidOperation(SpanData span) {
    String operation = span.getName();
    if (operation == null || operation.equals(UNKNOWN_OPERATION)) {
      return false;
    }
    if (isKeyPresent(span, HTTP_METHOD)) {
      String httpMethod = span.getAttributes().get(HTTP_METHOD);
      return !operation.equals(httpMethod);
    }
    return true;
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
   *   <li>GraphQL - Special case, if {@link GraphqlIncubatingAttributes#GRAPHQL_OPERATION_TYPE} is
   *       present, we use it for RemoteOperation and set RemoteService to {@link #GRAPHQL}.
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
   * if the selected attributes are still producing the UnknownRemoteService or
   * UnknownRemoteOperation, `net.peer.name`, `net.peer.port`, `net.peer.sock.addr` and
   * `net.peer.sock.port` will be used to derive the RemoteService. And `http.method` and `http.url`
   * will be used to derive the RemoteOperation.
   */
  private static void setRemoteServiceAndOperation(SpanData span, AttributesBuilder builder) {
    String remoteService = UNKNOWN_REMOTE_SERVICE;
    String remoteOperation = UNKNOWN_REMOTE_OPERATION;
    if (isKeyPresent(span, AWS_REMOTE_SERVICE) || isKeyPresent(span, AWS_REMOTE_OPERATION)) {
      remoteService = getRemoteService(span, AWS_REMOTE_SERVICE);
      remoteOperation = getRemoteOperation(span, AWS_REMOTE_OPERATION);
    } else if (isKeyPresent(span, RPC_SERVICE) || isKeyPresent(span, RPC_METHOD)) {
      remoteService = getRemoteService(span, RPC_SERVICE);
      remoteOperation = getRemoteOperation(span, RPC_METHOD);
    } else if (isKeyPresent(span, DB_SYSTEM) || isKeyPresent(span, DB_OPERATION)) {
      remoteService = getRemoteService(span, DB_SYSTEM);
      remoteOperation = getRemoteOperation(span, DB_OPERATION);
    } else if (isKeyPresent(span, FAAS_INVOKED_NAME) || isKeyPresent(span, FAAS_TRIGGER)) {
      remoteService = getRemoteService(span, FAAS_INVOKED_NAME);
      remoteOperation = getRemoteOperation(span, FAAS_TRIGGER);
    } else if (isKeyPresent(span, MESSAGING_SYSTEM) || isKeyPresent(span, MESSAGING_OPERATION)) {
      remoteService = getRemoteService(span, MESSAGING_SYSTEM);
      remoteOperation = getRemoteOperation(span, MESSAGING_OPERATION);
    } else if (isKeyPresent(span, GRAPHQL_OPERATION_TYPE)) {
      remoteService = GRAPHQL;
      remoteOperation = getRemoteOperation(span, GRAPHQL_OPERATION_TYPE);
    }

    // Peer service takes priority as RemoteService over everything but AWS Remote.
    if (isKeyPresent(span, PEER_SERVICE) && !isKeyPresent(span, AWS_REMOTE_SERVICE)) {
      remoteService = getRemoteService(span, PEER_SERVICE);
    }

    // try to derive RemoteService and RemoteOperation from the other related attributes
    if (remoteService.equals(UNKNOWN_REMOTE_SERVICE)) {
      remoteService = generateRemoteService(span);
    }
    if (remoteOperation.equals(UNKNOWN_REMOTE_OPERATION)) {
      remoteOperation = generateRemoteOperation(span);
    }

    builder.put(AWS_REMOTE_SERVICE, remoteService);
    builder.put(AWS_REMOTE_OPERATION, remoteOperation);
  }

  /**
   * When span name is not meaningful(null, unknown or http_method value) as operation name for http
   * use cases. Will try to extract the operation name from http target string
   */
  private static String generateIngressOperation(SpanData span) {
    String operation = UNKNOWN_OPERATION;
    if (isKeyPresent(span, HTTP_TARGET)) {
      String httpTarget = span.getAttributes().get(HTTP_TARGET);
      // get the first part from API path string as operation value
      // the more levels/parts we get from API path the higher chance for getting high cardinality
      // data
      if (httpTarget != null) {
        operation = extractApiPathValue(httpTarget);
        if (isKeyPresent(span, HTTP_METHOD)) {
          String httpMethod = span.getAttributes().get(HTTP_METHOD);
          if (httpMethod != null) {
            operation = httpMethod + " " + operation;
          }
        }
      }
    }
    return operation;
  }

  /**
   * When the remote call operation is undetermined for http use cases, will try to extract the
   * remote operation name from http url string
   */
  private static String generateRemoteOperation(SpanData span) {
    String remoteOperation = UNKNOWN_REMOTE_OPERATION;
    if (isKeyPresent(span, HTTP_URL)) {
      String httpUrl = span.getAttributes().get(HTTP_URL);
      try {
        URL url;
        if (httpUrl != null) {
          url = new URL(httpUrl);
          remoteOperation = extractApiPathValue(url.getPath());
        }
      } catch (MalformedURLException e) {
        logger.log(Level.FINEST, "invalid http.url attribute: ", httpUrl);
      }
    }
    if (isKeyPresent(span, HTTP_METHOD)) {
      String httpMethod = span.getAttributes().get(HTTP_METHOD);
      remoteOperation = httpMethod + " " + remoteOperation;
    }
    if (remoteOperation.equals(UNKNOWN_REMOTE_OPERATION)) {
      logUnknownAttribute(AWS_REMOTE_OPERATION, span);
    }
    return remoteOperation;
  }

  /**
   * Extract the first part from API http target if it exists
   *
   * @param httpTarget http request target string value. Eg, /payment/1234
   * @return the first part from the http target. Eg, /payment
   */
  private static String extractApiPathValue(String httpTarget) {
    if (httpTarget == null || httpTarget.isEmpty()) {
      return "/";
    }
    String[] paths = httpTarget.split("/");
    if (paths.length > 1) {
      return "/" + paths[1];
    }
    return "/";
  }

  private static String generateRemoteService(SpanData span) {
    String remoteService = UNKNOWN_REMOTE_SERVICE;
    if (isKeyPresent(span, NET_PEER_NAME)) {
      remoteService = getRemoteService(span, NET_PEER_NAME);
      if (isKeyPresent(span, NET_PEER_PORT)) {
        Long port = span.getAttributes().get(NET_PEER_PORT);
        remoteService += ":" + port;
      }
    } else if (isKeyPresent(span, NET_SOCK_PEER_ADDR)) {
      remoteService = getRemoteService(span, NET_SOCK_PEER_ADDR);
      if (isKeyPresent(span, NET_SOCK_PEER_PORT)) {
        Long port = span.getAttributes().get(NET_SOCK_PEER_PORT);
        remoteService += ":" + port;
      }
    } else {
      logUnknownAttribute(AWS_REMOTE_SERVICE, span);
    }
    return remoteService;
  }

  /** Span kind is needed for differentiating metrics in the EMF exporter */
  private static void setSpanKind(SpanData span, AttributesBuilder builder) {
    String spanKind = span.getKind().name();
    builder.put(AWS_SPAN_KIND, spanKind);
  }

  private static boolean isKeyPresent(SpanData span, AttributeKey<?> key) {
    return span.getAttributes().get(key) != null;
  }

  private static String getRemoteService(SpanData span, AttributeKey<String> remoteServiceKey) {
    String remoteService = span.getAttributes().get(remoteServiceKey);
    if (remoteService == null) {
      remoteService = UNKNOWN_REMOTE_SERVICE;
    }
    return remoteService;
  }

  private static String getRemoteOperation(SpanData span, AttributeKey<String> remoteOperationKey) {
    String remoteOperation = span.getAttributes().get(remoteOperationKey);
    if (remoteOperation == null) {
      remoteOperation = UNKNOWN_REMOTE_OPERATION;
    }
    return remoteOperation;
  }

  private static void logUnknownAttribute(AttributeKey<String> attributeKey, SpanData span) {
    String[] params = {
      attributeKey.getKey(), span.getKind().name(), span.getSpanContext().getSpanId()
    };
    logger.log(Level.FINEST, "No valid {0} value found for {1} span {2}", params);
  }
}
