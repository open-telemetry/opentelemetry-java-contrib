package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import opamp.proto.AgentToServer;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * <p>AgentToServer request builder appender. Each implementation should match one of the
 * AgentToServer fields and ensure the field is added to a request.
 */
public interface AgentToServerAppender {
  /**
   * Appends its data to the builder.
   *
   * @param builder The AgentToServer message builder.
   */
  void appendTo(AgentToServer.Builder builder);
}
