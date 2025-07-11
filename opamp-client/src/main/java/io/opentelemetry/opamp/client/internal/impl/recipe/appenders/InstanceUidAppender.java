package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import okio.ByteString;
import opamp.proto.AgentToServer;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class InstanceUidAppender implements AgentToServerAppender {
  private final Supplier<byte[]> instanceUid;

  public static InstanceUidAppender create(Supplier<byte[]> instanceUid) {
    return new InstanceUidAppender(instanceUid);
  }

  private InstanceUidAppender(Supplier<byte[]> instanceUid) {
    this.instanceUid = instanceUid;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.instance_uid(ByteString.of(instanceUid.get()));
  }
}
