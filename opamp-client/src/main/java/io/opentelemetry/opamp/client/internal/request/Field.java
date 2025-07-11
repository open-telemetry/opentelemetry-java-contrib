package io.opentelemetry.opamp.client.internal.request;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * <p>List of supported <a
 * href="https://github.com/open-telemetry/opamp-spec/blob/main/specification.md#agenttoserver-message">AgentToServer</a>
 * message fields.
 */
public enum Field {
  INSTANCE_UID,
  SEQUENCE_NUM,
  AGENT_DESCRIPTION,
  CAPABILITIES,
  EFFECTIVE_CONFIG,
  REMOTE_CONFIG_STATUS,
  AGENT_DISCONNECT,
  FLAGS
}
