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
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Add the {@link OtelExecutionListener} to the lifecycle of the Maven execution */
@Named
@Singleton
public final class OtelLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  private static final Logger logger = LoggerFactory.getLogger(OtelLifecycleParticipant.class);

  private final OpenTelemetrySdkService openTelemetrySdkService;

  private final OtelExecutionListener otelExecutionListener;

  private final OtelTransferListener otelTransferListener;

  /**
   * Manually instantiate {@link OtelExecutionListener} and hook it in the Maven build lifecycle
   * because Maven Sisu doesn't load it when Maven Plexus did.
   */
  @Inject
  OtelLifecycleParticipant(
      OpenTelemetrySdkService openTelemetrySdkService, SpanRegistry spanRegistry) {
    this.openTelemetrySdkService = openTelemetrySdkService;
    this.otelExecutionListener = new OtelExecutionListener(spanRegistry, openTelemetrySdkService);
    this.otelTransferListener = new OtelTransferListener(spanRegistry, openTelemetrySdkService);
  }

  @Override
  public void afterSessionStart(MavenSession session) {
    // TODO transfers happen before afterProjectsRead() - not sure I understand the issue in the
    // comment of afterProjectsRead()
    if (openTelemetrySdkService.isTransferInstrumentationEnabled()) {
      registerTransferListener(session);
    }
  }

  /**
   * For an unknown reason, {@link #afterProjectsRead(MavenSession)} is invoked when the module is
   * declared as an extension in pom.xml but {@link #afterSessionStart(MavenSession)} is not
   * invoked.
   */
  @Override
  public void afterProjectsRead(MavenSession session) {
    registerExecutionListener(session);
  }

  void registerExecutionListener(MavenSession session) {
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
          "OpenTelemetry: OpenTelemetry extension registered as execution listener. InitialExecutionListener: {}",
          initialExecutionListener);
    }
  }

  void registerTransferListener(MavenSession session) {
    RepositorySystemSession repositorySession = session.getRepositorySession();
    TransferListener initialTransferListener = repositorySession.getTransferListener();
    if (initialTransferListener instanceof ChainedTransferListener
        || initialTransferListener instanceof OtelTransferListener) {
      // already initialized
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension already registered as transfer listener, skip.");
    } else if (initialTransferListener == null) {
      setTransferListener(this.otelTransferListener, repositorySession, session);
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension registered as transfer listener. No transfer listener initially defined");
    } else {
      setTransferListener(
          new ChainedTransferListener(this.otelTransferListener, initialTransferListener),
          repositorySession,
          session);
      logger.debug(
          "OpenTelemetry: OpenTelemetry extension registered as transfer listener. InitialTransferListener: {}",
          initialTransferListener);
    }
  }

  void setTransferListener(
      TransferListener transferListener,
      RepositorySystemSession repositorySession,
      MavenSession session) {
    if (repositorySession instanceof DefaultRepositorySystemSession) {
      ((DefaultRepositorySystemSession) repositorySession).setTransferListener(transferListener);
    } else {
      logger.warn("OpenTelemetry: Cannot set transfer listener");
    }
  }

  @Override
  public void afterSessionEnd(MavenSession session) {
    // Workaround https://issues.apache.org/jira/browse/MNG-8217
    // close OpenTelemetry SDK in `afterSessionEnd()`
    logger.debug("OpenTelemetry: After Maven session end, close OpenTelemetry SDK");
    openTelemetrySdkService.close();
  }
}
