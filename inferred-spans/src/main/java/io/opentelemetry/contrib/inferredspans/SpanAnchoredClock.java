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

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

public class SpanAnchoredClock {
  private final WeakConcurrentMap<Span, Long> nanoTimeOffsetMap = new WeakConcurrentMap.WithInlinedExpunction<>();

  public void onSpanStart(ReadWriteSpan started, Context parentContext) {
    Span parent = Span.fromContext(parentContext);
    Long parentAnchor = parent == null ? null : nanoTimeOffsetMap.get(parent);
    if (parentAnchor != null) {
      nanoTimeOffsetMap.put(started, parentAnchor);
    } else {
      long spanLatency = started.getLatencyNanos();
      long clockNowNanos = nanoTime();
      long spanStartNanos = started.toSpanData().getStartEpochNanos();
      long anchor = spanStartNanos - spanLatency - clockNowNanos;
      nanoTimeOffsetMap.put(started, anchor);
    }
  }

  public long nanoTime() {
    return System.nanoTime();
  }

  /**
   * Returns a value which allows to translate timestamps obtained via {@link #nanoTime()} to
   * absolute epoche time stamps based on the start-time of the given span.
   *
   * <p>This anchor value can be used in {@link #toEpochNanos(long, long)} to perform the
   * translation.
   */
  public long getAnchor(Span span) {
    return nanoTimeOffsetMap.get(span);
  }

  /**
   * Translates a timestamp obtained via {@link #nanoTime()} with the help of an anchor obtained via
   * {@link #getAnchor(Span)} to an absolute nano-precision epoch timestamp.
   */
  public long toEpochNanos(long anchor, long recordedNanoTime) {
    return recordedNanoTime + anchor;
  }
}
