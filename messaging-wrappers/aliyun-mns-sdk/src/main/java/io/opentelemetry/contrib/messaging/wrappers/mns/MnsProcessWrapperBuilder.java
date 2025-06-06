/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns;

import io.opentelemetry.contrib.messaging.wrappers.DefaultMessagingProcessWrapperBuilder;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MnsProcessRequest;

public class MnsProcessWrapperBuilder<REQUEST extends MnsProcessRequest>
    extends DefaultMessagingProcessWrapperBuilder<REQUEST> {

  MnsProcessWrapperBuilder() {
    super();
    super.textMapGetter = MnsTextMapGetter.create();
  }
}
