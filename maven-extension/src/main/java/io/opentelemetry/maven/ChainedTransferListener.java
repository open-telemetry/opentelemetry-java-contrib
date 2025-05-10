/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

/**
 * Util class to chain multiple {@link TransferListener} as Maven APIs don't offer this capability.
 */
final class ChainedTransferListener implements TransferListener {

  private final List<TransferListener> listeners;

  /**
   * @param listeners {@code null} values are filtered
   */
  ChainedTransferListener(TransferListener... listeners) {
    this.listeners = Arrays.stream(listeners).filter(e -> e != null).collect(Collectors.toList());
  }

  @Override
  public void transferInitiated(TransferEvent event) throws TransferCancelledException {
    for (TransferListener listener : this.listeners) {
      listener.transferInitiated(event);
    }
  }

  @Override
  public void transferStarted(TransferEvent event) throws TransferCancelledException {
    for (TransferListener listener : this.listeners) {
      listener.transferStarted(event);
    }
  }

  @Override
  public void transferProgressed(TransferEvent event) throws TransferCancelledException {
    for (TransferListener listener : this.listeners) {
      listener.transferProgressed(event);
    }
  }

  @Override
  public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
    for (TransferListener listener : this.listeners) {
      listener.transferCorrupted(event);
    }
  }

  @Override
  public void transferSucceeded(TransferEvent event) {
    for (TransferListener listener : this.listeners) {
      listener.transferSucceeded(event);
    }
  }

  @Override
  public void transferFailed(TransferEvent event) {
    for (TransferListener listener : this.listeners) {
      listener.transferFailed(event);
    }
  }
}
