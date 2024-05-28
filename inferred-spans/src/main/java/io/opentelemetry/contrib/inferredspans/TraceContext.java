/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.opentelemetry.contrib.inferredspans;

import co.elastic.otel.common.util.HexUtils;
import io.opentelemetry.contrib.inferredspans.pooling.Recyclable;
import io.opentelemetry.contrib.inferredspans.util.ByteUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.ReadableSpan;
import javax.annotation.Nullable;

/**
 * A mutable (and therefore recyclable) class storing the relevant bits of {@link SpanContext} for
 * generating inferred spans. Also stores a clock-anchor for the corresponding span obtained via
 * {@link SpanAnchoredClock#getAnchor(Span)}.
 */
public class TraceContext implements Recyclable {

  public static final int SERIALIZED_LENGTH = 16 + 8 + 1 + 1 + 8 + 8;
  private long traceIdLow;
  private long traceIdHigh;
  private long id;

  private boolean hasParentId;
  private long parentId;
  private byte flags;

  private long clockAnchor;

  public TraceContext() {}

  // For testing only
  static TraceContext fromSpanContextWithZeroClockAnchor(
      SpanContext ctx, @Nullable String parentSpanId) {
    TraceContext result = new TraceContext();
    result.fillFromSpanContext(ctx, parentSpanId);
    result.clockAnchor = 0L;
    return result;
  }

  private void fillFromSpanContext(SpanContext ctx, @Nullable String parentSpanId) {
    id = HexUtils.hexToLong(ctx.getSpanId(), 0);
    traceIdHigh = HexUtils.hexToLong(ctx.getTraceId(), 0);
    traceIdLow = HexUtils.hexToLong(ctx.getTraceId(), 16);
    if (parentSpanId != null) {
      hasParentId = true;
      parentId = HexUtils.hexToLong(parentSpanId, 0);
    } else {
      hasParentId = false;
    }
    flags = ctx.getTraceFlags().asByte();
  }

  public SpanContext toOtelSpanContext(StringBuilder temporaryBuilder) {
    temporaryBuilder.setLength(0);
    HexUtils.appendLongAsHex(traceIdHigh, temporaryBuilder);
    HexUtils.appendLongAsHex(traceIdLow, temporaryBuilder);
    String traceIdStr = temporaryBuilder.toString();

    temporaryBuilder.setLength(0);
    HexUtils.appendLongAsHex(id, temporaryBuilder);
    String idStr = temporaryBuilder.toString();

    return SpanContext.create(
        traceIdStr, idStr, TraceFlags.fromByte(flags), TraceState.getDefault());
  }

  public long getSpanId() {
    return id;
  }

  public boolean idEquals(@Nullable TraceContext o) {
    if (o == null) {
      return false;
    }
    return id == o.id;
  }

  public static long getSpanId(byte[] serialized) {
    return ByteUtils.getLong(serialized, 16);
  }

  public void deserialize(byte[] serialized) {
    traceIdLow = ByteUtils.getLong(serialized, 0);
    traceIdHigh = ByteUtils.getLong(serialized, 8);
    id = ByteUtils.getLong(serialized, 16);
    flags = serialized[24];
    hasParentId = serialized[25] != 0;
    parentId = ByteUtils.getLong(serialized, 26);
    clockAnchor = ByteUtils.getLong(serialized, 34);
  }

  public static long getParentId(byte[] serializedTraceContext) {
    boolean hasParent = serializedTraceContext[25] != 0;
    if (!hasParent) {
      return 0L;
    }
    return ByteUtils.getLong(serializedTraceContext, 26);
  }

  public boolean traceIdAndIdEquals(byte[] otherSerialized) {
    long otherTraceIdLow = ByteUtils.getLong(otherSerialized, 0);
    if (otherTraceIdLow != traceIdLow) {
      return false;
    }
    long otherTraceIdHigh = ByteUtils.getLong(otherSerialized, 8);
    if (otherTraceIdHigh != traceIdHigh) {
      return false;
    }
    long otherId = ByteUtils.getLong(otherSerialized, 16);
    return id == otherId;
  }

  public static void serialize(Span span, long clockAnchor, byte[] buffer) {
    SpanContext ctx = span.getSpanContext();
    SpanContext parentSpanCtx = SpanContext.getInvalid();
    if (span instanceof ReadableSpan) {
      parentSpanCtx = ((ReadableSpan) span).getParentSpanContext();
    }

    long id = HexUtils.hexToLong(ctx.getSpanId(), 0);
    long traceIdHigh = HexUtils.hexToLong(ctx.getTraceId(), 0);
    long traceIdLow = HexUtils.hexToLong(ctx.getTraceId(), 16);
    byte flags = ctx.getTraceFlags().asByte();
    ByteUtils.putLong(buffer, 0, traceIdLow);
    ByteUtils.putLong(buffer, 8, traceIdHigh);
    ByteUtils.putLong(buffer, 16, id);
    buffer[24] = flags;
    if (parentSpanCtx.isValid()) {
      buffer[25] = 1;
      ByteUtils.putLong(buffer, 26, HexUtils.hexToLong(parentSpanCtx.getSpanId(), 0));
    } else {
      buffer[25] = 0;
      ByteUtils.putLong(buffer, 26, 0);
    }
    ByteUtils.putLong(buffer, 34, clockAnchor);
  }

  public void serialize(byte[] buffer) {
    ByteUtils.putLong(buffer, 0, traceIdLow);
    ByteUtils.putLong(buffer, 8, traceIdHigh);
    ByteUtils.putLong(buffer, 16, id);
    buffer[24] = flags;
    if (hasParentId) {
      buffer[25] = 1;
      ByteUtils.putLong(buffer, 26, parentId);
    } else {
      buffer[25] = 0;
      ByteUtils.putLong(buffer, 26, 0);
    }
    ByteUtils.putLong(buffer, 34, clockAnchor);
  }

  public byte[] serialize() {
    byte[] result = new byte[SERIALIZED_LENGTH];
    serialize(result);
    return result;
  }

  @Override
  public void resetState() {
    traceIdLow = 0;
    traceIdHigh = 0;
    id = 0;
    flags = 0;
    clockAnchor = 0;
  }

  public long getClockAnchor() {
    return clockAnchor;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    SpanContext otelSpanCtx = toOtelSpanContext(result);
    result.setLength(0);
    result.append(otelSpanCtx).append("(clock-anchor: ").append(clockAnchor).append(')');
    return result.toString();
  }
}
