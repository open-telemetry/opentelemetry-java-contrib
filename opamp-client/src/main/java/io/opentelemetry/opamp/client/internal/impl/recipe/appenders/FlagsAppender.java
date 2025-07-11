package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import opamp.proto.AgentToServer;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class FlagsAppender implements AgentToServerAppender {
  private final Supplier<Long> flags;

  public static FlagsAppender create(Supplier<Long> flags) {
    return new FlagsAppender(flags);
  }

  private FlagsAppender(Supplier<Long> flags) {
    this.flags = flags;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.flags(flags.get());
  }
}
