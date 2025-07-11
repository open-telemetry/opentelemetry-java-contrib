package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import opamp.proto.AgentToServer;
import opamp.proto.RemoteConfigStatus;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class RemoteConfigStatusAppender implements AgentToServerAppender {
  private final Supplier<RemoteConfigStatus> remoteConfigStatus;

  public static RemoteConfigStatusAppender create(Supplier<RemoteConfigStatus> remoteConfigStatus) {
    return new RemoteConfigStatusAppender(remoteConfigStatus);
  }

  private RemoteConfigStatusAppender(Supplier<RemoteConfigStatus> remoteConfigStatus) {
    this.remoteConfigStatus = remoteConfigStatus;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.remote_config_status(remoteConfigStatus.get());
  }
}
