/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Add the {@link OtelExecutionListener} to the lifecycle of the Maven execution */
@Named
@Singleton
public final class OtelLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  private static final Logger logger = LoggerFactory.getLogger(OtelLifecycleParticipant.class);

  private final OtelExecutionListener otelExecutionListener;

  /**
   * Manually instantiate {@link OtelExecutionListener} and hook it in the Maven build lifecycle
   * because Maven Sisu doesn't load it when Maven Plexus did.
   */
  @Inject
  OtelLifecycleParticipant(
      OpenTelemetrySdkService openTelemetrySdkService, SpanRegistry spanRegistry) {
    this.otelExecutionListener = new OtelExecutionListener(spanRegistry, openTelemetrySdkService);
  }

  /**
   * For an unknown reason, {@link #afterProjectsRead(MavenSession)} is invoked when the module is
   * declared as an extension in pom.xml but {@link #afterSessionStart(MavenSession)} is not
   * invoked.
   */
  @Override
  public void afterProjectsRead(MavenSession session) {
    ExecutionListener initialExecutionListener = session.getRequest().getExecutionListener();
    if (initialExecutionListener instanceof ChainedExecutionListener
        || initialExecutionListener instanceof OtelExecutionListener) {
      // already initialized
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension already registered as execution listener, skip.");
    } else if (initialExecutionListener == null) {
      session.getRequest().setExecutionListener(this.otelExecutionListener);
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension registered as execution listener. No execution listener initially defined");
    } else {
      session
          .getRequest()
          .setExecutionListener(
              new ChainedExecutionListener(this.otelExecutionListener, initialExecutionListener));
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension registered as execution listener. InitialExecutionListener: "
              + initialExecutionListener);
    }
    logger.debug("OpenTelemetry: afterProjectsRead");
  }

  @Override
  public void afterSessionEnd(MavenSession session) {
    logger.debug("OpenTelemetry: afterSessionEnd");
  }
}
