package io.opentelemetry.opamp.client.internal.state;

import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * <p>Abstraction for a client request field that carries data. Each implementation can only be
 * linked to one type of client request field, which is provided in its {@link #getFieldType()}
 * method.
 */
public interface State<T> extends Supplier<T> {

  Field getFieldType();

  @Nonnull
  default T mustGet() {
    return Objects.requireNonNull(get());
  }

  final class InstanceUid extends InMemoryState<byte[]> {
    public InstanceUid(byte[] initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.INSTANCE_UID;
    }
  }

  final class SequenceNum extends InMemoryState<Long> {
    public SequenceNum(Long initialValue) {
      super(initialValue);
    }

    public void increment() {
      set(mustGet() + 1);
    }

    @Override
    public Field getFieldType() {
      return Field.SEQUENCE_NUM;
    }
  }

  final class AgentDescription extends InMemoryState<opamp.proto.AgentDescription> {
    public AgentDescription(opamp.proto.AgentDescription initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.AGENT_DESCRIPTION;
    }
  }

  final class Capabilities extends InMemoryState<Long> {
    public Capabilities(Long initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.CAPABILITIES;
    }
  }

  final class RemoteConfigStatus extends InMemoryState<opamp.proto.RemoteConfigStatus> {

    public RemoteConfigStatus(opamp.proto.RemoteConfigStatus initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.REMOTE_CONFIG_STATUS;
    }
  }

  final class Flags extends InMemoryState<Long> {

    public Flags(Long initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.FLAGS;
    }
  }

  abstract class EffectiveConfig extends ObservableState<opamp.proto.EffectiveConfig> {
    @Override
    public final Field getFieldType() {
      return Field.EFFECTIVE_CONFIG;
    }
  }
}
