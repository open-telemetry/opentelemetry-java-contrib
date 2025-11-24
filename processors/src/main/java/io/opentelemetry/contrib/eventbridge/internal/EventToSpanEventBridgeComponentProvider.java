/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.eventbridge.internal;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.eventbridge.EventToSpanEventBridge;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;

/**
 * Declarative configuration SPI implementation for {@link EventToSpanEventBridge}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class EventToSpanEventBridgeComponentProvider implements ComponentProvider {

  @Override
  public Class<LogRecordProcessor> getType() {
    return LogRecordProcessor.class;
  }

  @Override
  public String getName() {
    return "event_to_span_event_bridge";
  }

  @Override
  public LogRecordProcessor create(DeclarativeConfigProperties config) {
    return EventToSpanEventBridge.create();
  }
}
