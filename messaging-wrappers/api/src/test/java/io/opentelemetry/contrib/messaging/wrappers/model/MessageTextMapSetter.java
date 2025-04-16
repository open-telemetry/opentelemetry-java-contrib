package io.opentelemetry.contrib.messaging.wrappers.model;

import io.opentelemetry.context.propagation.TextMapSetter;

import javax.annotation.Nullable;

public class MessageTextMapSetter implements TextMapSetter<Message> {

  public static TextMapSetter<Message> create() {
    return new MessageTextMapSetter();
  }

  @Override
  public void set(@Nullable Message carrier, String key, String value) {
    if (carrier == null) {
      return;
    }
    carrier.getHeaders().put(key, value);
  }
}
