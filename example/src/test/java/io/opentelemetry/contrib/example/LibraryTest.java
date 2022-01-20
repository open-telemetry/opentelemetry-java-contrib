/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LibraryTest {

  @Test
  void myMethod() {
    Library library = new Library();
    assertThat(library.myMethod()).isTrue();
  }
}
