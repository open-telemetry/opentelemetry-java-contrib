/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.contrib.inferredspans.util.ThreadUtils;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class ProfilingActivationListener implements Closeable {

  static {
    // ContextStorage.addWrapper must
    // * happen before anyone accesses any Context
    // * happen exactly once
    // The "exactly" once part is why we use a static initializer:
    // If an Otel-SDK is created and immediately shutdown again and if we create another SDK
    // afterwards, we might accidentally register the wrapper twice
    ContextStorage.addWrapper(ContextStorageWrapper::new);
  }

  // For testing only
  static void ensureInitialized() {
    // does nothing but ensures that the static initializer ran
  }

  // In normal use-cases there is only one ProfilingActivationListener active or zero
  // (e.g. after SDK shutdown). However, in theory nothing prevents users from starting
  // two SDKs at the same time, so it is safest to use a List here.
  @SuppressWarnings("NonFinalStaticField")
  private static volatile List<ProfilingActivationListener> activeListeners =
      Collections.emptyList();

  private static class ContextStorageWrapper implements ContextStorage {

    private final ContextStorage delegate;

    private ContextStorageWrapper(ContextStorage delegate) {
      this.delegate = delegate;
    }

    @Override
    public Scope attach(Context toAttach) {
      List<ProfilingActivationListener> listeners = activeListeners;
      if (listeners.isEmpty()) {
        // no unnecessary allocations when no listener is active
        return delegate.attach(toAttach);
      }
      Span attached = spanFromContextNullSafe(toAttach);
      Span oldCtx = spanFromContextNullSafe(delegate.current());
      for (ProfilingActivationListener listener : listeners) {
        listener.beforeActivate(oldCtx, attached);
      }
      Scope delegateScope = delegate.attach(toAttach);
      return () -> {
        delegateScope.close();
        Span newCtx = spanFromContextNullSafe(delegate.current());
        for (ProfilingActivationListener listener : listeners) {
          listener.afterDeactivate(attached, newCtx);
        }
      };
    }

    private static Span spanFromContextNullSafe(@Nullable Context context) {
      if (context == null) {
        return Span.getInvalid();
      }
      return Span.fromContext(context);
    }

    @Nullable
    @Override
    public Context current() {
      return delegate.current();
    }

    @Override
    public Context root() {
      return delegate.root();
    }
  }

  private final SamplingProfiler profiler;

  private ProfilingActivationListener(SamplingProfiler profiler) {
    this.profiler = profiler;
  }

  public static ProfilingActivationListener register(SamplingProfiler profiler) {
    ProfilingActivationListener result = new ProfilingActivationListener(profiler);
    synchronized (ProfilingActivationListener.class) {
      List<ProfilingActivationListener> listenersList = new ArrayList<>(activeListeners);
      listenersList.add(result);
      activeListeners = Collections.unmodifiableList(listenersList);
    }
    return result;
  }

  @Override
  public void close() {
    synchronized (ProfilingActivationListener.class) {
      List<ProfilingActivationListener> listenersList = new ArrayList<>(activeListeners);
      listenersList.remove(this);
      activeListeners = Collections.unmodifiableList(listenersList);
    }
  }

  public void beforeActivate(Span oldContext, Span newContext) {
    if (newContext.getSpanContext().isValid()
        && newContext.getSpanContext().isSampled()
        && !ThreadUtils.isVirtual(Thread.currentThread())) {
      profiler.onActivation(newContext, oldContext.getSpanContext().isValid() ? oldContext : null);
    }
  }

  public void afterDeactivate(Span deactivatedContext, Span newContext) {
    if (deactivatedContext.getSpanContext().isValid()
        && deactivatedContext.getSpanContext().isSampled()
        && !ThreadUtils.isVirtual(Thread.currentThread())) {
      profiler.onDeactivation(
          deactivatedContext, newContext.getSpanContext().isValid() ? newContext : null);
    }
  }
}
