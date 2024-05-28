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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

public class FixedClock extends SpanAnchoredClock {

  private long nanoTime = -1L;

  @Override
  public void onSpanStart(ReadWriteSpan started, Context parentContext) {}

  @Override
  public long nanoTime() {
    if (nanoTime == -1L) {
      return System.nanoTime();
    }
    return nanoTime;
  }

  @Override
  public long getAnchor(Span parent) {
    return 0;
  }

  @Override
  public long toEpochNanos(long anchor, long recordedNanoTime) {
    return recordedNanoTime;
  }

  public void setNanoTime(long nanoTime) {
    this.nanoTime = nanoTime;
  }
}
