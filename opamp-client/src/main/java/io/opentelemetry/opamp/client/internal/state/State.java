/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.state;

import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import opamp.proto.ComponentHealth;

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

  /** In-memory state for the instance UID field. */
  final class InstanceUid extends InMemoryState<byte[]> {
    public InstanceUid(byte[] initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.INSTANCE_UID;
    }
  }

  /** In-memory state for the sequence number field. */
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

  /** In-memory state for the agent description field. */
  final class AgentDescription extends InMemoryState<opamp.proto.AgentDescription> {
    public AgentDescription(opamp.proto.AgentDescription initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.AGENT_DESCRIPTION;
    }
  }

  /** In-memory state for the capabilities field. */
  final class Capabilities extends InMemoryState<Long> {
    public Capabilities(Long initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.CAPABILITIES;
    }
  }

  /** In-memory state for the custom capabilities field. */
  final class CustomCapabilities extends InMemoryState<opamp.proto.CustomCapabilities> {
    public CustomCapabilities(@Nullable opamp.proto.CustomCapabilities initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.CUSTOM_CAPABILITIES;
    }
  }

  /** In-memory state for the health field. */
  final class Health extends InMemoryState<ComponentHealth> {
    public Health(@Nullable ComponentHealth initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.HEALTH;
    }
  }

  /** In-memory state for the remote config status field. */
  final class RemoteConfigStatus extends InMemoryState<opamp.proto.RemoteConfigStatus> {

    public RemoteConfigStatus(opamp.proto.RemoteConfigStatus initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.REMOTE_CONFIG_STATUS;
    }
  }

  /** In-memory state for the flags field. */
  final class Flags extends InMemoryState<Long> {

    public Flags(Long initialValue) {
      super(initialValue);
    }

    @Override
    public Field getFieldType() {
      return Field.FLAGS;
    }
  }

  /** Observable state for the effective config field. */
  abstract class EffectiveConfig extends ObservableState<opamp.proto.EffectiveConfig> {
    @Override
    public final Field getFieldType() {
      return Field.EFFECTIVE_CONFIG;
    }
  }
}
