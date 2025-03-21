package io.opentelemetry.contrib.messaging.wrappers.semconv;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * An interface to expose messaging properties for the pre-defined process wrapper.
 *
 * <p>Inspired from <a href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/main/java/io/opentelemetry/instrumentation/api/incubator/semconv/messaging/MessagingAttributesGetter.java>MessagingAttributesGetter</a>.
 */
public interface MessagingProcessRequest {

  String getSystem();

  String getDestination();

  String getDestinationTemplate();

  boolean isTemporaryDestination();

  boolean isAnonymousDestination();

  String getConversationId();

  Long getMessageBodySize();

  Long getMessageEnvelopeSize();

  String getMessageId();

  default String getOperationName() {
      return "process";
  }

  default String getOperationType() {
      return "process";
  }

  default String getConsumerGroupName() {
      return null;
  }

  default String getDestinationSubscriptionName() {
      return null;
  }

  default String getClientId() {
      return null;
  }

  default Long getBatchMessageCount() {
      return null;
  }

  default String getDestinationPartitionId() {
      return null;
  }

  /**
   * Extracts all values of header named {@code name} from the request, or an empty list if there
   * were none.
   *
   * <p>Implementations of this method <b>must not</b> return a null value; an empty list should be
   * returned instead.
   */
  default List<String> getMessageHeader(String name) {
        return emptyList();
    }
}
