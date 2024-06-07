/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.contrib.inferredspans.collections.LongList;

/** List for maintaining pairs of (spanId,parentIds) both represented as longs. */
public class ChildList {

  // this list contains the (spanId,parentIds) flattened
  private LongList idsWithParentIds = new LongList();

  public void add(long id, long parentId) {
    idsWithParentIds.add(id);
    idsWithParentIds.add(parentId);
  }

  public long getId(int index) {
    return idsWithParentIds.get(index * 2);
  }

  public long getParentId(int index) {
    return idsWithParentIds.get(index * 2 + 1);
  }

  public int getSize() {
    return idsWithParentIds.getSize() / 2;
  }

  public void addAll(ChildList other) {
    idsWithParentIds.addAll(other.idsWithParentIds);
  }

  public void clear() {
    idsWithParentIds.clear();
  }

  public boolean isEmpty() {
    return getSize() == 0;
  }

  public void removeLast() {
    int size = idsWithParentIds.getSize();
    idsWithParentIds.remove(size - 1);
    idsWithParentIds.remove(size - 2);
  }
}
