/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;
import io.opentelemetry.semconv.incubating.UrlIncubatingAttributes;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Don't mark this class as {@link javax.inject.Named} and {@link javax.inject.Singleton} because
 * Maven Sisu doesn't automatically load instance of {@link ExecutionListener} as Maven Extension
 * hooks the same way Maven Plexus did so we manually hook this instance of {@link
 * ExecutionListener} through the {@link OtelLifecycleParticipant#afterProjectsRead(MavenSession)}.
 */
public final class OtelTransferListener extends AbstractTransferListener {

  private static final Logger logger = LoggerFactory.getLogger(OtelTransferListener.class);

  private final SpanRegistry spanRegistry;

  private final OpenTelemetrySdkService openTelemetrySdkService;

  private final Map<String, Optional<URI>> repositoryUriMapping = new ConcurrentHashMap<>();

  OtelTransferListener(SpanRegistry spanRegistry, OpenTelemetrySdkService openTelemetrySdkService) {
    this.spanRegistry = spanRegistry;
    this.openTelemetrySdkService = openTelemetrySdkService;
  }

  @Override
  public void transferInitiated(TransferEvent event) {
    logger.debug("OpenTelemetry: OtelTransferListener#transferInitiated({})", event);

    String httpRequestMethod;
    switch (event.getRequestType()) {
      case PUT:
        httpRequestMethod = "PUT";
        break;
      case GET:
        httpRequestMethod = "GET";
        break;
      case GET_EXISTENCE:
        httpRequestMethod = "HEAD";
        break;
      default:
        logger.warn(
            "OpenTelemetry: Unknown request type {} for event {}", event.getRequestType(), event);
        httpRequestMethod = event.getRequestType().name();
    }

    String urlTemplate =
        event.getResource().getRepositoryUrl()
            + "$groupId/$artifactId/$version/$artifactId-$version.$classifier";

    String spanName = httpRequestMethod + " " + urlTemplate;

    // Build an HTTP client span as the http call itself is not instrumented.
    SpanBuilder spanBuilder =
        this.openTelemetrySdkService
            .getTracer()
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, httpRequestMethod)
            .setAttribute(
                UrlAttributes.URL_PATH,
                event.getResource().getRepositoryUrl() + event.getResource().getResourceName())
            .setAttribute(UrlIncubatingAttributes.URL_TEMPLATE, urlTemplate)
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_TRANSFER_TYPE, event.getRequestType().name())
            .setAttribute(
                MavenOtelSemanticAttributes.MAVEN_RESOURCE_NAME,
                event.getResource().getResourceName());

    repositoryUriMapping
        .computeIfAbsent(
            event.getResource().getRepositoryUrl(),
            str -> {
              try {
                return str.isEmpty() ? Optional.empty() : Optional.of(new URI(str));
              } catch (URISyntaxException e) {
                return Optional.empty();
              }
            })
        .ifPresent(
            uri -> {
              spanBuilder.setAttribute(ServerAttributes.SERVER_ADDRESS, uri.getHost());
              if (uri.getPort() != -1) {
                spanBuilder.setAttribute(ServerAttributes.SERVER_PORT, uri.getPort());
              }
              // prevent ever increasing size
              if (repositoryUriMapping.size() > 128) {
                repositoryUriMapping.clear();
              }
            });
    spanRegistry.putSpan(spanBuilder.startSpan(), event);
  }

  @Override
  public void transferSucceeded(TransferEvent event) {
    logger.debug("OpenTelemetry: OtelTransferListener#transferSucceeded({})", event);

    Optional.ofNullable(spanRegistry.removeSpan(event))
        .ifPresent(
            span -> {
              span.setStatus(StatusCode.OK);
              finish(span, event);
            });
  }

  @Override
  public void transferFailed(TransferEvent event) {
    logger.debug("OpenTelemetry: OtelTransferListener#transferFailed({})", event);

    Optional.ofNullable(spanRegistry.removeSpan(event)).ifPresent(span -> fail(span, event));
  }

  @Override
  public void transferCorrupted(TransferEvent event) {
    logger.debug("OpenTelemetry: OtelTransferListener#transferCorrupted({})", event);

    Optional.ofNullable(spanRegistry.removeSpan(event)).ifPresent(span -> fail(span, event));
  }

  void finish(Span span, TransferEvent event) {
    switch (event.getRequestType()) {
      case PUT:
        span.setAttribute(
            HttpIncubatingAttributes.HTTP_REQUEST_BODY_SIZE, event.getTransferredBytes());
        break;
      case GET:
      case GET_EXISTENCE:
        span.setAttribute(
            HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE, event.getTransferredBytes());
        break;
    }
    span.end();
  }

  void fail(Span span, TransferEvent event) {
    span.setStatus(
        StatusCode.ERROR,
        Optional.ofNullable(event.getException()).map(Exception::getMessage).orElse("n/a"));
    finish(span, event);
  }
}
