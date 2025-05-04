/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.maven.semconv.MavenOtelSemanticAttributes;
import java.util.Locale;
import java.util.Optional;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
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

  OtelTransferListener(SpanRegistry spanRegistry, OpenTelemetrySdkService openTelemetrySdkService) {
    this.spanRegistry = spanRegistry;
    this.openTelemetrySdkService = openTelemetrySdkService;
  }

  /**
   * Starts a span to collect transfers that occur before regular execution spans can serve as
   * parents.
   */
  public void startTransferRoot() {
    io.opentelemetry.context.Context context =
        openTelemetrySdkService
            .getPropagators()
            .getTextMapPropagator()
            .extract(
                io.opentelemetry.context.Context.current(),
                System.getenv(),
                new ToUpperCaseTextMapGetter());

    // TODO question: is this the root span name we want?
    String spanName = "Transfer: global";
    logger.debug("OpenTelemetry: Start span: {}", spanName);
    Span transferSpan =
        this.openTelemetrySdkService
            .getTracer()
            .spanBuilder(spanName)
            .setParent(context)
            .setSpanKind(SpanKind.SERVER)
            .startSpan();
    spanRegistry.setRootSpan(transferSpan);
  }

  /** Ends the root span. */
  public void endTransferRoot() {
    spanRegistry.getRootSpanNotNull().end();
  }

  @Override
  public void transferInitiated(TransferEvent event) {
    ResourceInformation info = createResourceInformation(event);

    logger.debug("OpenTelemetry: Maven transfer initiated: span {}:{}", info.type, info.url);

    SpanBuilder spanBuilder =
        this.openTelemetrySdkService
            .getTracer()
            .spanBuilder(info.type + ":" + info.url)
            .setParent(
                Context.current()
                    .with(Span.wrap(spanRegistry.getRootSpanNotNull().getSpanContext())))
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_TRANSFER_URL, info.url)
            .setAttribute(MavenOtelSemanticAttributes.MAVEN_TRANSFER_TYPE, info.type);

    spanRegistry.putSpan(spanBuilder.startSpan(), event);
  }

  @Override
  public void transferSucceeded(TransferEvent event) {
    ResourceInformation info = createResourceInformation(event);

    logger.debug("OpenTelemetry: Maven transfer succeeded: span {}:{}", info.type, info.url);

    Optional.ofNullable(spanRegistry.removeSpan(event))
        .ifPresent(
            span -> {
              span.setStatus(StatusCode.OK);
              finish(span, event);
            });
  }

  @Override
  public void transferFailed(TransferEvent event) {
    ResourceInformation info = createResourceInformation(event);

    logger.debug("OpenTelemetry: Maven transfer failed: span {}:{}", info.type, info.url);

    Optional.ofNullable(spanRegistry.removeSpan(event)).ifPresent(span -> fail(span, event));
  }

  @Override
  public void transferCorrupted(TransferEvent event) {
    ResourceInformation info = createResourceInformation(event);

    logger.debug("OpenTelemetry: Maven transfer corrupted: span {}:{}", info.type, info.url);

    Optional.ofNullable(spanRegistry.removeSpan(event)).ifPresent(span -> fail(span, event));
  }

  void finish(Span span, TransferEvent event) {
    span.setAttribute(
        MavenOtelSemanticAttributes.MAVEN_TRANSFER_SIZE,
        Long.toString(event.getTransferredBytes()));
    span.end();
  }

  void fail(Span span, TransferEvent event) {
    span.setStatus(
        StatusCode.ERROR,
        Optional.ofNullable(event.getException()).map(Exception::getMessage).orElse("n/a"));
    finish(span, event);
  }

  ResourceInformation createResourceInformation(TransferEvent event) {
    TransferResource resource = event.getResource();
    return new ResourceInformation(
        resource.getRepositoryUrl() + resource.getResourceName(),
        event.getRequestType().toString().toLowerCase(Locale.ROOT));
  }

  private static class ResourceInformation {
    protected final String url;
    protected final String type;

    ResourceInformation(String url, String type) {
      this.url = url;
      this.type = type;
    }
  }
}
