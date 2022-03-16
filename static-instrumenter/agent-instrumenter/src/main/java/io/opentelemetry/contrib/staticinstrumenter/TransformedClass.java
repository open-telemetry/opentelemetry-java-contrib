/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

public class TransformedClass {
  private final byte[] classcode;
  private final String name;

  public TransformedClass(String name, byte[] classcode) {
    this.classcode = classcode;
    this.name = name;
  }

  public byte[] getClasscode() {
    return classcode;
  }

  public String getName() {
    return name;
  }
}
