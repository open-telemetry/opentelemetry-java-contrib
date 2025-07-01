package io.opentelemetry.opamp.client.internal.request.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;

final class DaemonThreadFactory implements ThreadFactory {
  private final ThreadFactory delegate = Executors.defaultThreadFactory();

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    Thread t = delegate.newThread(r);
    try {
      t.setDaemon(true);
    } catch (SecurityException e) {
      // Well, we tried.
    }
    return t;
  }
}
