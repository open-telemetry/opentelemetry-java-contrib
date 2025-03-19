package io.opentelemetry.contrib.messaging.wrappers.semconv;

/**
 * An interface to expose messaging properties for the pre-defined process wrapper.
 *
 * <p>Only be created on demand.
 *
 * <p>Inspired from <a href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/main/java/io/opentelemetry/instrumentation/api/incubator/semconv/messaging/MessagingAttributesGetter.java>MessagingAttributesGetter</a>.
 */
public interface MessagingProcessResponse<T> {

  default String getMessageId() {
      return null;
  }

  default Long getBatchMessageCount() {
      return null;
  }

  T getOriginalResponse();
}
