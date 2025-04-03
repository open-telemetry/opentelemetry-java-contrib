package io.opentelemetry.contrib.messaging.wrappers.mns;

import io.opentelemetry.contrib.messaging.wrappers.DefaultMessagingProcessWrapperBuilder;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessRequest;

public class MNSProcessWrapperBuilder<REQUEST extends MNSProcessRequest>
    extends DefaultMessagingProcessWrapperBuilder<REQUEST> {

  MNSProcessWrapperBuilder() {
    super();
    super.textMapGetter = MNSTextMapGetter.create();
  }
}
