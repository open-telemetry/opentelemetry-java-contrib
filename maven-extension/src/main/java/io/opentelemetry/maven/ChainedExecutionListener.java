/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;

/**
 * Util class to chain multiple {@link ExecutionListener} as Maven APIs don't offer this capability.
 */
final class ChainedExecutionListener implements ExecutionListener {

  private final List<ExecutionListener> listeners;

  /** @param listeners, {@code null} values are filtered */
  ChainedExecutionListener(List<ExecutionListener> listeners) {
    this.listeners = listeners.stream().filter(e -> e != null).collect(Collectors.toList());
  }

  @Override
  public void projectDiscoveryStarted(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.projectDiscoveryStarted(event);
    }
  }

  @Override
  public void sessionStarted(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.sessionStarted(event);
    }
  }

  @Override
  public void sessionEnded(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.sessionEnded(event);
    }
  }

  @Override
  public void projectSkipped(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.projectSkipped(event);
    }
  }

  @Override
  public void projectStarted(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.projectStarted(event);
    }
  }

  @Override
  public void projectSucceeded(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.projectSucceeded(event);
    }
  }

  @Override
  public void projectFailed(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.projectFailed(event);
    }
  }

  @Override
  public void mojoSkipped(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.mojoSkipped(event);
    }
  }

  @Override
  public void mojoStarted(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.mojoStarted(event);
    }
  }

  @Override
  public void mojoSucceeded(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.mojoSucceeded(event);
    }
  }

  @Override
  public void mojoFailed(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.mojoFailed(event);
    }
  }

  @Override
  public void forkStarted(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.forkStarted(event);
    }
  }

  @Override
  public void forkSucceeded(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.forkSucceeded(event);
    }
  }

  @Override
  public void forkFailed(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.forkFailed(event);
    }
  }

  @Override
  public void forkedProjectStarted(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.forkedProjectStarted(event);
    }
  }

  @Override
  public void forkedProjectSucceeded(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.forkedProjectSucceeded(event);
    }
  }

  @Override
  public void forkedProjectFailed(ExecutionEvent event) {
    for (ExecutionListener listener : this.listeners) {
      listener.forkedProjectFailed(event);
    }
  }

  @Override
  public String toString() {
    return "ChainedExecutionListener{"
        + listeners.stream().map(l -> l.toString()).collect(Collectors.joining(", "))
        + '}';
  }
}
