/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.example;

public class Library {

  public boolean myMethod() {
    return true;
  }

  @SuppressWarnings("SystemOut")
  public static void main(String... args) {
    System.out.println("ExampleLibrary.main");
  }
}
