/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal;

import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.request.service.RequestService;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public final class Experimental {

  @Nullable
  private static volatile BiConsumer<OpampClientBuilder, Function<URI, RequestService>>
      setServiceFactory;

  /**
   * Sets factory function for creating {@link RequestService} instances form a given server URI.
   */
  public static void setServiceFactory(
      OpampClientBuilder builder, Function<URI, RequestService> serviceFactory) {
    if (setServiceFactory != null) {
      setServiceFactory.accept(builder, serviceFactory);
    }
  }

  public static void internalSetServiceFactory(
      BiConsumer<OpampClientBuilder, Function<URI, RequestService>> setServiceFactory) {
    Experimental.setServiceFactory = setServiceFactory;
  }

  private Experimental() {}
}
